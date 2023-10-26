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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.web.client;

import reactor.netty.http.client.HttpClient;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateCustomizer;
import org.springframework.cloud.commons.httpclient.HttpClientConfiguration;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.instrument.web.client.HttpClientBeanPostProcessor;
import io.micrometer.tracing.instrument.web.client.LazyTraceClientHttpRequestInterceptor;
import io.micrometer.tracing.instrument.web.client.TraceDefaultOAuth2UserServiceCustomizer;
import io.micrometer.tracing.instrument.web.client.TraceRequestHttpHeadersFilter;
import io.micrometer.tracing.instrument.web.client.TraceResponseHttpHeadersFilter;
import io.micrometer.tracing.instrument.web.client.TraceRestTemplateBeanPostProcessor;
import io.micrometer.tracing.instrument.web.client.TraceRestTemplateCustomizer;
import io.micrometer.tracing.instrument.web.client.TraceUserInfoRestTemplateCustomizer;
import io.micrometer.tracing.instrument.web.client.TraceWebClientBeanPostProcessor;
import io.micrometer.tracing.instrument.web.client.UserInfoRestTemplateCustomizerBeanPostProcessor;
import io.micrometer.tracing.instrument.web.mvc.TracingClientHttpRequestInterceptor;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables span information propagation when using
 * {@link RestTemplate}.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalnOnSleuthWebClient
@ConditionalOnBean(Tracer.class)
@AutoConfigureBefore(HttpClientConfiguration.class)
@AutoConfigureAfter(BraveAutoConfiguration.class)
class TraceWebClientAutoConfiguration {

	@ConditionalOnClass(RestTemplate.class)
	static class RestTemplateConfig {

		@Bean
		public TracingClientHttpRequestInterceptor tracingClientHttpRequestInterceptor(
				CurrentTraceContext currentTraceContext, HttpClientHandler httpClientHandler) {
			return (TracingClientHttpRequestInterceptor) TracingClientHttpRequestInterceptor.create(currentTraceContext,
					httpClientHandler);
		}

		@Bean
		@Order
		TraceRestTemplateCustomizer traceRestTemplateCustomizer(BeanFactory beanFactory) {
			return new TraceRestTemplateCustomizer(new LazyTraceClientHttpRequestInterceptor(beanFactory));
		}

		@Bean
		static TraceRestTemplateBeanPostProcessor traceRestTemplateBeanPostProcessor(ListableBeanFactory beanFactory) {
			return new TraceRestTemplateBeanPostProcessor(beanFactory);
		}

		@ConditionalOnClass(org.springframework.vault.client.RestTemplateCustomizer.class)
		@ConditionalOnProperty(value = "spring.sleuth.vault.enabled", matchIfMissing = true)
		static class VaultRestTemplateCustomizerConfiguration {

			@Bean
			@Order(Ordered.HIGHEST_PRECEDENCE)
			org.springframework.vault.client.RestTemplateCustomizer traceVaultRestTemplateCustomizer(
					TraceRestTemplateCustomizer restTemplateCustomizer) {
				return restTemplateCustomizer::customize;
			}

		}

	}

	@ConditionalOnClass(HttpHeadersFilter.class)
	@ConditionalOnMissingClass("reactor.netty.http.client.HttpClient")
	static class HttpHeadersFilterConfig {

		@Bean
		HttpHeadersFilter traceRequestHttpHeadersFilter(Tracer tracer, HttpClientHandler handler,
				Propagator propagator) {
			return new TraceRequestHttpHeadersFilter(tracer, handler, propagator);
		}

		@Bean
		HttpHeadersFilter traceResponseHttpHeadersFilter(Tracer tracer, HttpClientHandler handler,
				Propagator propagator) {
			return new TraceResponseHttpHeadersFilter(tracer, handler, propagator);
		}

	}

	@ConditionalOnClass(HttpClient.class)
	static class NettyConfiguration {

		@Bean
		static HttpClientBeanPostProcessor httpClientBeanPostProcessor(ConfigurableApplicationContext springContext) {
			return new HttpClientBeanPostProcessor(springContext);
		}

	}

	@ConditionalOnClass(WebClient.class)
	@ConditionalOnProperty(value = "spring.sleuth.web.webclient.enabled", matchIfMissing = true)
	static class WebClientConfig {

		@Bean
		static TraceWebClientBeanPostProcessor traceWebClientBeanPostProcessor(
				ConfigurableApplicationContext springContext) {
			return new TraceWebClientBeanPostProcessor(springContext);
		}

		@ConditionalOnClass(org.springframework.vault.client.WebClientCustomizer.class)
		@ConditionalOnProperty(value = "spring.sleuth.vault.enabled", matchIfMissing = true)
		static class VaultWebClientCustomizerConfiguration {

			@Bean
			@Order(Ordered.HIGHEST_PRECEDENCE)
			org.springframework.vault.client.WebClientCustomizer traceVaultWebClientCustomizer(
					ConfigurableApplicationContext springContext) {
				return webClientBuilder -> new TraceWebClientBeanPostProcessor(springContext)
						.postProcessAfterInitialization(webClientBuilder, "");
			}

		}

	}

	@ConditionalOnClass({ UserInfoRestTemplateCustomizer.class, OAuth2RestTemplate.class })
	@Deprecated // Use Spring-Security OAuth2 support
	protected static class TraceOAuthConfiguration {

		@Bean
		static UserInfoRestTemplateCustomizerBeanPostProcessor userInfoRestTemplateCustomizerBeanPostProcessor(
				BeanFactory beanFactory) {
			return new UserInfoRestTemplateCustomizerBeanPostProcessor(beanFactory);
		}

		@Bean
		@ConditionalOnMissingBean
		static UserInfoRestTemplateCustomizer traceUserInfoRestTemplateCustomizer(BeanFactory beanFactory) {
			return new TraceUserInfoRestTemplateCustomizer(beanFactory);
		}

	}

	@ConditionalOnClass(DefaultOAuth2UserService.class)
	protected static class TraceSpringSecurityOAuth2Configuration {

		@Bean
		static BeanPostProcessor traceDefaultOAuth2UserServiceBeanPostProcessor(BeanFactory beanFactory) {
			return new BeanPostProcessor() {
				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof DefaultOAuth2UserService service) {
						new TraceDefaultOAuth2UserServiceCustomizer(beanFactory)
								.customize(service);
					}
					return bean;
				}
			};
		}

	}

}
