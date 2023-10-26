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

package io.micrometer.tracing.instrument.web.servlet;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.http.HttpServerResponse;

public final class TracingFilter implements Filter {

	final ServletRuntime servlet = ServletRuntime.get();

	final CurrentTraceContext currentTraceContext;

	final HttpServerHandler handler;

	public static TracingFilter create(CurrentTraceContext currentTraceContext, HttpServerHandler httpServerHandler) {
		return new TracingFilter(currentTraceContext, httpServerHandler);
	}

	TracingFilter(CurrentTraceContext currentTraceContext, HttpServerHandler httpServerHandler) {
		this.currentTraceContext = currentTraceContext;
		this.handler = httpServerHandler;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = servlet.httpServletResponse(response);

		// Prevent duplicate spans for the same request
		TraceContext context = (TraceContext) request.getAttribute(TraceContext.class.getName());
		if (context != null) {
			// A forwarded request might end up on another thread, so make sure it is
			// scoped
			CurrentTraceContext.Scope scope = currentTraceContext.maybeScope(context);
			try {
				chain.doFilter(request, response);
			}
			finally {
				scope.close();
			}
			return;
		}

		Span span = handler.handleReceive(new HttpServletRequestWrapper(req));

		// Add attributes for explicit access to customization or span context
		request.setAttribute(SpanCustomizer.class.getName(), span);
		request.setAttribute(TraceContext.class.getName(), span.context());
		SendHandled sendHandled = new SendHandled();
		request.setAttribute(SendHandled.class.getName(), sendHandled);

		Throwable error = null;
		CurrentTraceContext.Scope scope = currentTraceContext.newScope(span.context());
		try {
			// any downstream code can see Tracer.currentSpan() or use
			// Tracer.currentSpanCustomizer()
			chain.doFilter(req, res);
		}
		catch (Throwable e) {
			error = e;
			throw e;
		}
		finally {
			// When async, even if we caught an exception, we don't have the final
			// response: defer
			if (servlet.isAsync(req)) {
				servlet.handleAsync(handler, req, res, span);
			}
			else if (sendHandled.compareAndSet(false, true)) {
				// we have a synchronous response or error: finish the span
				HttpServerResponse responseWrapper = HttpServletResponseWrapper.create(req, res, error);
				handler.handleSend(responseWrapper, span);
			}
			scope.close();
		}
	}

	@Override
	public void destroy() {
	}

	@Override
	public void init(FilterConfig filterConfig) {
	}

	/**
	 * Special type used to ensure handleSend is only called once.
	 */
	static final class SendHandled extends AtomicBoolean {

	}

}
