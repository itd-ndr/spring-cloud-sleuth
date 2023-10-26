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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.web.client.feign;

import feign.Client;
import feign.Feign;
import feign.okhttp.OkHttpClient;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;
import org.springframework.cloud.openfeign.FeignContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.instrument.web.client.feign.FeignContextBeanPostProcessor;
import io.micrometer.tracing.instrument.web.client.feign.OkHttpFeignClientBeanPostProcessor;
import io.micrometer.tracing.instrument.web.client.feign.SleuthFeignBuilder;
import io.micrometer.tracing.instrument.web.client.feign.TraceFeignAspect;
import io.micrometer.tracing.instrument.web.client.feign.TraceFeignBuilderBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables span information propagation when using Feign.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(value = "spring.sleuth.feign.enabled", matchIfMissing = true)
@ConditionalOnClass({Client.class, FeignContext.class})
@ConditionalOnBean(Tracer.class)
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class TraceFeignClientAutoConfiguration {

	@Bean
	TraceFeignAspect traceFeignAspect(BeanFactory beanFactory) {
		return new TraceFeignAspect(beanFactory);
	}

	@ConditionalOnProperty(name = "spring.sleuth.feign.processor.enabled", matchIfMissing = true)
	protected static class FeignBeanPostProcessorConfiguration {

		@Bean
		static FeignContextBeanPostProcessor feignContextBeanPostProcessor(BeanFactory beanFactory) {
			return new FeignContextBeanPostProcessor(beanFactory);
		}

		@Bean
		static TraceFeignBuilderBeanPostProcessor traceFeignBuilderBeanPostProcessor(BeanFactory beanFactory) {
			return new TraceFeignBuilderBeanPostProcessor(beanFactory);
		}

	}

	@ConditionalOnClass(OkHttpClient.class)
	protected static class OkHttpClientFeignBeanPostProcessorConfiguration {

		@Bean
		static OkHttpFeignClientBeanPostProcessor okHttpFeignClientBeanPostProcessor(BeanFactory beanFactory) {
			return new OkHttpFeignClientBeanPostProcessor(beanFactory);
		}

	}

	@Conditional(CircuitBreakerMissingOrDisabledCondition.class)
	protected static class CircuitBreakerMissingConfiguration {

		@Bean
		@ConditionalOnMissingBean
		@Scope("prototype")
		Feign.Builder feignBuilder(BeanFactory beanFactory) {
			return SleuthFeignBuilder.builder(beanFactory);
		}

	}

	private static class CircuitBreakerMissingOrDisabledCondition extends AnyNestedCondition {

		CircuitBreakerMissingOrDisabledCondition() {
			super(ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnMissingClass("org.springframework.cloud.client.circuitbreaker.CircuitBreaker")
		static class NoCircuitBreakerClassFound {

		}

		@ConditionalOnProperty(value = "feign.circuitbreaker.enabled", havingValue = "false", matchIfMissing = true)
		static class CircuitBreakerDisabled {

		}

	}

}
