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

package io.micrometer.tracing.instrument.web.tomcat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletException;

import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.http.HttpServerRequest;
import io.micrometer.tracing.http.HttpServerResponse;
import io.micrometer.tracing.tracer.SimpleCurrentTraceContext;
import io.micrometer.tracing.tracer.SimpleSpan;

import static org.assertj.core.api.BDDAssertions.then;

class TraceValveTests {

	SimpleSpan simpleSpan = new SimpleSpan();

	AtomicInteger startCounter = new AtomicInteger();

	AtomicInteger endCounter = new AtomicInteger();

	HttpServerHandler httpServerHandler = new HttpServerHandler() {

		@Override
		public SimpleSpan handleReceive(HttpServerRequest request) {
			startCounter.incrementAndGet();
			return simpleSpan.start();
		}

		@Override
		public void handleSend(HttpServerResponse response, Span span) {
			endCounter.incrementAndGet();
			span.end();
		}
	};

	TraceValve traceValve = new TraceValve(this.httpServerHandler, new SimpleCurrentTraceContext());

	@Test
	void should_populate_tracecontext_attribute_for_tracing_filter_to_reuse() throws ServletException, IOException {
		Request request = request();

		this.traceValve.invoke(request, new Response());

		then(request.getAttribute(TraceContext.class.getName())).isNotNull();
		thenSpanIsStartedAndStopped();
	}

	@Test
	void should_have_async_supported_by_default() throws ServletException, IOException {
		TraceValve traceValve = new TraceValve((HttpServerHandler) null, null);

		BDDAssertions.then(traceValve.isAsyncSupported()).isTrue();
	}

	private void thenSpanIsStartedAndStopped() {
		then(simpleSpan.started).isTrue();
		then(startCounter.get()).isEqualTo(1);
		then(simpleSpan.ended).isTrue();
		then(endCounter.get()).isEqualTo(1);
	}

	@Test
	void should_populate_tracecontext_attribute_for_tracing_filter_to_reuse_when_there_is_another_valve_in_chain()
			throws ServletException, IOException {
		Request request = request();

		new TraceValve(this.httpServerHandler, new SimpleCurrentTraceContext()) {
			@Override
			public Valve getNext() {
				return new MyValve();
			}
		}.invoke(request, new Response());

		then(request.getAttribute(TraceContext.class.getName())).isNotNull();
		thenSpanIsStartedAndStopped();
	}

	@Test
	void should_not_generate_a_new_span_when_one_already_present() throws ServletException, IOException {
		Request request = request();

		new TraceValve(this.httpServerHandler, new SimpleCurrentTraceContext()) {
			@Override
			public Valve getNext() {
				return new TraceValve(httpServerHandler, new SimpleCurrentTraceContext());
			}
		}.invoke(request, new Response());

		then(request.getAttribute(TraceContext.class.getName())).isNotNull();
		thenSpanIsStartedAndStopped();
	}

	private Request request() {
		Request request = new Request(new Connector());
		request.setCoyoteRequest(new org.apache.coyote.Request());
		return request;
	}

}

class MyValve extends ValveBase {

	@Override
	public void invoke(Request request, Response response) throws IOException, ServletException {

	}

}
