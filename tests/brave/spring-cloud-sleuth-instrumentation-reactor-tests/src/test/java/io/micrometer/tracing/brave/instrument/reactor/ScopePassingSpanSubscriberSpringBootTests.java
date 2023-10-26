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

package io.micrometer.tracing.brave.instrument.reactor;

import brave.sampler.Sampler;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.brave.BraveTestSpanHandler;
import io.micrometer.tracing.brave.bridge.BraveAccessor;
import io.micrometer.tracing.test.TestSpanHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Like {@link ScopePassingSpanSubscriberTests}, except this tests wiring with spring boot
 * config.
 */
@SpringBootTest(classes = ScopePassingSpanSubscriberSpringBootTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class ScopePassingSpanSubscriberSpringBootTests
		extends io.micrometer.tracing.instrument.reactor.ScopePassingSpanSubscriberSpringBootTests {

	TraceContext context = BraveAccessor
			.traceContext(brave.propagation.TraceContext.newBuilder().traceId(1).spanId(1).sampled(true).build());

	TraceContext context2 = BraveAccessor
			.traceContext(brave.propagation.TraceContext.newBuilder().traceId(1).spanId(2).sampled(true).build());

	@Override
	protected TraceContext context() {
		return this.context;
	}

	@Override
	protected TraceContext context2() {
		return this.context2;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class Config {

		@Bean
		TestSpanHandler testSpanHandlerSupplier(brave.test.TestSpanHandler testSpanHandler) {
			return new BraveTestSpanHandler(testSpanHandler);
		}

		@Bean
		Sampler alwaysSampler() {
			return Sampler.ALWAYS_SAMPLE;
		}

		@Bean
		brave.test.TestSpanHandler braveTestSpanHandler() {
			return new brave.test.TestSpanHandler();
		}

	}

}
