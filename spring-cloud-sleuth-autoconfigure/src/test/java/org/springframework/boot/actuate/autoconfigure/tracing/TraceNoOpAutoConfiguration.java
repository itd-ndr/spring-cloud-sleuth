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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.SpanCustomizer;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.instrument.web.ConditionalOnSleuthWeb;
import org.springframework.boot.actuate.autoconfigure.tracing.instrument.web.client.ConditionalnOnSleuthWebClient;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to enable tracing via Spring Cloud Sleuth.
 *
 * @author Spencer Gibb
 * @author Marcin Grzejszczak
 * @author Tim Ysewyn
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnSleuth
@ConditionalOnProperty("spring.sleuth.noop.enabled")
@AutoConfigureBefore(BraveAutoConfiguration.class)
@Import(TraceConfiguration.class)
public class TraceNoOpAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Tracer defaultTracer() {
		return new NoOpTracer();
	}

	@Bean
	Propagator defaultPropagator() {
		return new NoOpPropagator();
	}

	@Bean
	CurrentTraceContext defaultCurrentTraceContext() {
		return new NoOpCurrentTraceContext();
	}

	@Bean
	SpanCustomizer defaultSpanCustomizer() {
		return new NoOpSpanCustomizer();
	}

	static class TraceHttpConfiguration {

		@Bean
		@ConditionalnOnSleuthWebClient
		HttpClientHandler defaultHttpClientHandler() {
			return new NoOpHttpClientHandler();
		}

		@Bean
		@ConditionalOnSleuthWeb
		HttpServerHandler defaultHttpServerHandler() {
			return new NoOpHttpServerHandler();
		}

	}

}

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Documented
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@interface ConditionalOnSleuth {

}
