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

package io.micrometer.tracing.brave.instrument.web;

import java.util.regex.Pattern;

import brave.http.HttpTracing;
import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import io.micrometer.tracing.brave.BraveTestTracing;
import io.micrometer.tracing.brave.bridge.BraveAccessor;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.instrument.web.servlet.TracingFilter;
import io.micrometer.tracing.test.TestTracingAware;
import io.micrometer.tracing.test.TracerAware;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Spencer Gibb
 */
public class TraceFilterTests extends io.micrometer.tracing.instrument.web.TraceFilterTests {

	BraveTestTracing testTracing;

	@Override
	public TestTracingAware tracerTest() {
		if (this.testTracing == null) {
			this.testTracing = new BraveTestTracing();
		}
		return this.testTracing;
	}

	@Override
	public HttpServerHandler httpServerHandler() {
		HttpTracing httpTracing = this.testTracing.httpTracingBuilder()
				.serverSampler(new SkipPatternHttpServerSampler(() -> Pattern.compile(""))).build();
		return BraveAccessor.httpServerHandler(brave.http.HttpServerHandler.create(httpTracing));
	}

	@Test
	void createsChildFromHeadersWhenJoinUnsupported() throws Exception {
		this.request = builder().header("b3", "0000000000000014-000000000000000a")
				.buildRequest(new MockServletContext());
		TracerAware aware = tracerTest().tracing();
		BraveTestTracing braveTestTracing = ((BraveTestTracing) aware);
		braveTestTracing.tracingBuilder(braveTestTracing.tracingBuilder().supportsJoin(false)).reset();

		TracingFilter.create(aware.currentTraceContext(), httpServerHandler()).doFilter(this.request, this.response,
				this.filterChain);

		BDDAssertions.then(tracerTest().tracing().tracer().currentSpan()).isNull();
		BDDAssertions.then(tracerTest().handler()).hasSize(1);
		BDDAssertions.then(tracerTest().handler().get(0).getParentId()).isEqualTo("000000000000000a");
	}

	@Test
	void samplesASpanDebugFlagWithInterceptor() throws Exception {
		this.request = builder().header("b3", "d").buildRequest(new MockServletContext());

		neverSampleFilter().doFilter(this.request, this.response, this.filterChain);

		BDDAssertions.then(this.tracer.currentSpan()).isNull();
		BDDAssertions.then(this.spans).hasSize(1);
		BDDAssertions.then(this.spans.get(0).getName()).isEqualTo("GET");
	}

	@Test
	void shouldNotStoreHttpStatusCodeWhenResponseCodeHasNotYetBeenSet() throws Exception {
		this.response.setStatus(0);
		this.filter.doFilter(this.request, this.response, this.filterChain);

		BDDAssertions.then(this.tracer.currentSpan()).isNull();
		BDDAssertions.then(this.spans).hasSize(1);
		BDDAssertions.then(this.spans.get(0).getTags()).doesNotContainKey("http.status_code");
	}

	@Test
	void samplesASpanRegardlessOfTheSamplerWhenDebugIsPresent() throws Exception {
		this.request = builder().header("b3", "d").buildRequest(new MockServletContext());

		neverSampleFilter().doFilter(this.request, this.response, this.filterChain);

		BDDAssertions.then(this.tracer.currentSpan()).isNull();
		BDDAssertions.then(this.spans).isNotEmpty();
	}

	@Test
	void startsNewTraceWithParentIdInHeaders() throws Exception {
		this.request = builder().header("b3", "0000000000000002-0000000000000003-1-000000000000000a")
				.buildRequest(new MockServletContext());

		this.filter.doFilter(this.request, this.response, this.filterChain);

		BDDAssertions.then(this.tracer.currentSpan()).isNull();
		BDDAssertions.then(this.spans).hasSize(1);
		BDDAssertions.then(this.spans.get(0).getSpanId()).isEqualTo("0000000000000003");
		BDDAssertions.then(this.spans.get(0).getTags()).containsEntry("http.path", "/").containsEntry("http.method",
				HttpMethod.GET.toString());
	}

}
