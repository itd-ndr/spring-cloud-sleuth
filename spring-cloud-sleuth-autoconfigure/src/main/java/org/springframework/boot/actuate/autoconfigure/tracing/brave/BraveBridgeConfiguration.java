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

package org.springframework.boot.actuate.autoconfigure.tracing.brave;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import brave.Tracing;
import brave.handler.SpanHandler;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.micrometer.tracing.SpanCustomizer;
import org.springframework.boot.actuate.autoconfigure.tracing.SleuthBaggageProperties;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveContextWrappingFunction;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BravePropagator;
import io.micrometer.tracing.brave.bridge.BraveSpanCustomizer;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import io.micrometer.tracing.brave.bridge.CompositePropagationFactorySupplier;
import io.micrometer.tracing.brave.bridge.CompositeSpanHandler;
import io.micrometer.tracing.brave.propagation.PropagationFactorySupplier;
import io.micrometer.tracing.exporter.SpanReporter;
import io.micrometer.tracing.instrument.reactor.ReactorSleuth;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SleuthPropagationProperties.class)
class BraveBridgeConfiguration {

	@Bean
	io.micrometer.tracing.Tracer braveTracer(brave.Tracer tracer,
			io.micrometer.tracing.CurrentTraceContext braveCurrentTraceContext) {
		return new BraveTracer(tracer, braveCurrentTraceContext, new BraveBaggageManager());
	}

	@Bean
	io.micrometer.tracing.CurrentTraceContext braveCurrentTraceContext(
			brave.propagation.CurrentTraceContext currentTraceContext) {
		return new BraveCurrentTraceContext(currentTraceContext);
	}

	@Bean
	SpanCustomizer braveSpanCustomizer(brave.SpanCustomizer spanCustomizer) {
		return new BraveSpanCustomizer(spanCustomizer);
	}

	@Bean
	Propagator bravePropagator(Tracing tracing) {
		return new BravePropagator(tracing);
	}

	@Bean
	@ConditionalOnMissingBean
	PropagationFactorySupplier compositePropagationFactorySupplier(BeanFactory beanFactory,
			SleuthBaggageProperties baggageProperties, SleuthPropagationProperties properties) {
		return new CompositePropagationFactorySupplier(beanFactory, baggageProperties.getLocalFields(),
				properties.getType());
	}

	// Name is important for sampling conditions
	@Bean(name = "traceCompositeSpanHandler")
	SpanHandler compositeSpanHandler(ObjectProvider<List<SpanExportingPredicate>> exporters,
			ObjectProvider<List<SpanReporter>> reporters) {
		return new CompositeSpanHandler(exporters.getIfAvailable(ArrayList::new),
				reporters.getIfAvailable(ArrayList::new));
	}

	@Bean
	@ConditionalOnClass(name = "reactor.util.context.Context")
	static BraveReactorContextBeanDefinitionRegistryPostProcessor braveReactorContextBeanDefinitionRegistryPostProcessor() {
		return new BraveReactorContextBeanDefinitionRegistryPostProcessor();
	}

	static class BraveReactorContextBeanDefinitionRegistryPostProcessor
			implements BeanDefinitionRegistryPostProcessor, Closeable {

		private static final Log log = LogFactory.getLog(BraveReactorContextBeanDefinitionRegistryPostProcessor.class);

		@Override
		public void close() throws IOException {
			ReactorSleuth.contextWrappingFunction = Function.identity();
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			ReactorSleuth.contextWrappingFunction = new BraveContextWrappingFunction();
			if (log.isDebugEnabled()) {
				log.debug("Wrapped Reactor's context into a Brave representation");
			}
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

		}

	}

}
