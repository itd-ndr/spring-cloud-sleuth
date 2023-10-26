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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.web;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.catalina.Valve;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.SpanNamer;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.instrument.web.TraceWebAspect;
import io.micrometer.tracing.instrument.web.mvc.SpanCustomizingAsyncHandlerInterceptor;
import io.micrometer.tracing.instrument.web.servlet.TracingFilter;
import io.micrometer.tracing.instrument.web.tomcat.TraceValve;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables tracing to HTTP requests.
 *
 * @author Marcin Grzejszczak
 * @author Spencer Gibb
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSleuthWeb
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(HandlerInterceptorAdapter.class)
@Import(SpanCustomizingAsyncHandlerInterceptor.class)
@ConditionalOnProperty(value = "spring.sleuth.web.servlet.enabled", matchIfMissing = true)
class TraceWebServletConfiguration {

	@Bean
	TraceWebAspect traceWebAspect(Tracer tracer, CurrentTraceContext currentTraceContext, SpanNamer spanNamer) {
		return new TraceWebAspect(tracer, currentTraceContext, spanNamer);
	}

	@Bean
	FilterRegistrationBean traceWebFilter(BeanFactory beanFactory, SleuthWebProperties webProperties) {
		FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean(new LazyTracingFilter(beanFactory));
		filterRegistrationBean.setDispatcherTypes(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.FORWARD,
				DispatcherType.INCLUDE, DispatcherType.REQUEST);
		filterRegistrationBean.setOrder(webProperties.getFilterOrder());
		return filterRegistrationBean;
	}

	/**
	 * Nested config that configures Web MVC if it's present (without adding a runtime
	 * dependency to it).
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebMvcConfigurer.class)
	@Import(TraceWebMvcConfigurer.class)
	protected static class TraceWebMvcAutoConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Valve.class, ConfigurableTomcatWebServerFactory.class })
	@ConditionalOnProperty(value = "spring.sleuth.web.tomcat.enabled", matchIfMissing = true)
	protected static class TraceTomcatConfiguration {

		static final String CUSTOMIZER_NAME = "traceTomcatWebServerFactoryCustomizer";

		@Bean(name = CUSTOMIZER_NAME)
		@Order(Ordered.HIGHEST_PRECEDENCE)
		WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> traceTomcatWebServerFactoryCustomizer(
				ObjectProvider<HttpServerHandler> httpServerHandler,
				ObjectProvider<CurrentTraceContext> currentTraceContext) {
			return factory -> factory.addEngineValves(new TraceValve(httpServerHandler, currentTraceContext));
		}

	}

	static final class LazyTracingFilter implements Filter {

		private final BeanFactory beanFactory;

		private Filter tracingFilter;

		LazyTracingFilter(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			tracingFilter().init(filterConfig);
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			tracingFilter().doFilter(request, response, chain);
		}

		@Override
		public void destroy() {
			tracingFilter().destroy();
		}

		private Filter tracingFilter() {
			if (this.tracingFilter == null) {
				this.tracingFilter = TracingFilter.create(this.beanFactory.getBean(CurrentTraceContext.class),
						this.beanFactory.getBean(HttpServerHandler.class));
			}
			return this.tracingFilter;
		}

	}

}
