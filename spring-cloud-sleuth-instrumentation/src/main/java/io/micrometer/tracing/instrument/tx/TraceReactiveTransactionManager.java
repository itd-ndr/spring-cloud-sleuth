/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micrometer.tracing.instrument.tx;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanAndScope;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.docs.AssertingSpan;
import org.springframework.transaction.ReactiveTransaction;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * A trace representation of a {@link ReactiveTransactionManager}.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
public class TraceReactiveTransactionManager implements ReactiveTransactionManager {

	private static final Log log = LogFactory.getLog(TraceReactiveTransactionManager.class);

	private final ReactiveTransactionManager delegate;

	private final BeanFactory beanFactory;

	private Tracer tracer;

	private CurrentTraceContext currentTraceContext;

	public TraceReactiveTransactionManager(ReactiveTransactionManager delegate, BeanFactory beanFactory) {
		this.delegate = delegate;
		this.beanFactory = beanFactory;
	}

	private Tracer tracer() {
		if (this.tracer == null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

	private CurrentTraceContext currentTraceContext() {
		if (this.currentTraceContext == null) {
			this.currentTraceContext = this.beanFactory.getBean(CurrentTraceContext.class);
		}
		return this.currentTraceContext;
	}

	@Override
	public Mono<ReactiveTransaction> getReactiveTransaction(TransactionDefinition definition)
			throws TransactionException {
		return Mono.deferContextual(contextView -> this.delegate.getReactiveTransaction(definition).map(tx -> {
			Span span = AssertingSpan.continueSpan(SleuthTxSpan.TX_SPAN, SleuthTxSpan.TX_SPAN.wrap(span(contextView)));
			if (tx.isNewTransaction() || span == null) {
				if (log.isDebugEnabled()) {
					log.debug("New transaction is required, span in context [" + span + "]");
				}
				if (span == null) {
					span = SleuthTxSpan.TX_SPAN.wrap(tracer().nextSpan()).name(SleuthTxSpan.TX_SPAN.getName()).start();
				}
				else {
					span = SleuthTxSpan.TX_SPAN.wrap(tracer().nextSpan(span)).name(SleuthTxSpan.TX_SPAN.getName())
							.start();
				}
				TracePlatformTransactionManagerTags.tag(span, definition, this.delegate.getClass());
			}
			else if (log.isDebugEnabled()) {
				log.debug("Will continue the transaction for span [" + span + "]");
			}
			Tracer.SpanInScope withSpan = tracer().withSpan(span);
			SpanAndScope spanAndScope = new SpanAndScope(span, withSpan);
			if (log.isDebugEnabled()) {
				log.debug("Got transaction for span [" + spanAndScope + "]");
			}
			return new TraceReactiveTransaction(tx, spanAndScope);
		}));
	}

	private Span span(reactor.util.context.ContextView contextView) {
		Span span = contextView.getOrDefault(Span.class, null);
		if (span == null) {
			TraceContext traceContext = contextView.getOrDefault(TraceContext.class, null);
			if (traceContext == null) {
				Span currentSpan = tracer().currentSpan();
				if (log.isDebugEnabled()) {
					log.debug("There's no Span or TraceContext in the reactor context. Current span is [" + currentSpan
							+ "]");
				}
				span = currentSpan;
			}
			else {
				span = spanFromContext(traceContext);
			}
		}
		return span;
	}

	private Span spanFromContext(TraceContext traceContext) {
		try (CurrentTraceContext.Scope scope = currentTraceContext().maybeScope(traceContext)) {
			return SleuthTxSpan.TX_SPAN.wrap(tracer().currentSpan()).start();
		}
	}

	@Override
	public Mono<Void> commit(ReactiveTransaction transaction) throws TransactionException {
		if (!(transaction instanceof TraceReactiveTransaction)) {
			return this.delegate.commit(transaction);
		}
		TraceReactiveTransaction reactiveTransaction = (TraceReactiveTransaction) transaction;
		SpanAndScope spanAndScope = reactiveTransaction.spanAndScope;
		Span span = spanAndScope.getSpan();
		if (log.isDebugEnabled()) {
			log.debug("Committing the transaction for span [" + spanAndScope + "]");
		}
		spanAndScope.getScope().close(); // Otherwise we have a leak
		return this.delegate.commit(reactiveTransaction.delegate)
				// TODO: Fix me when this is resolved in Reactor
				// .doOnSubscribe(__ -> spanAndScope.getScope().close())
				.doOnError(span::error).doOnSuccess(signalType -> spanAndScope.close());
	}

	@Override
	public Mono<Void> rollback(ReactiveTransaction transaction) throws TransactionException {
		if (!(transaction instanceof TraceReactiveTransaction)) {
			return this.delegate.rollback(transaction);
		}
		TraceReactiveTransaction reactiveTransaction = (TraceReactiveTransaction) transaction;
		SpanAndScope spanAndScope = reactiveTransaction.spanAndScope;
		Span span = spanAndScope.getSpan();
		if (log.isDebugEnabled()) {
			log.debug("Rolling back the transaction for span [" + spanAndScope + "]");
		}
		spanAndScope.getScope().close(); // Otherwise we have a leak
		return this.delegate.rollback(reactiveTransaction.delegate)
				// TODO: Fix me when this is resolved in Reactor
				// .doOnSubscribe(__ -> scope.close())
				.doOnError(span::error).doFinally(signalType -> spanAndScope.close());
	}

	static class TraceReactiveTransaction implements ReactiveTransaction {

		final ReactiveTransaction delegate;

		final SpanAndScope spanAndScope;

		TraceReactiveTransaction(ReactiveTransaction delegate, SpanAndScope spanAndScope) {
			this.delegate = delegate;
			this.spanAndScope = spanAndScope;
		}

		@Override
		public boolean isNewTransaction() {
			return this.delegate.isNewTransaction();
		}

		@Override
		public void setRollbackOnly() {
			this.delegate.setRollbackOnly();
		}

		@Override
		public boolean isRollbackOnly() {
			return this.delegate.isRollbackOnly();
		}

		@Override
		public boolean isCompleted() {
			return this.delegate.isCompleted();
		}

	}

}
