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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.mongodb;

import java.util.ArrayList;
import java.util.List;

import com.mongodb.MongoClientSettings;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import io.micrometer.tracing.instrument.mongodb.TraceAllTypesMongoClientSettingsBuilderCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceMongoClientSettingsBuilderCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceMongoClusterIdSpanCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceMongoSocketAddressSpanCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceMongoSpanCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceReactiveMongoClientSettingsBuilderCustomizer;
import io.micrometer.tracing.instrument.mongodb.TraceSynchronousMongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables MongoDb span information propagation.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(BraveAutoConfiguration.class)
@AutoConfigureBefore(MongoAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.sleuth.mongodb.enabled", matchIfMissing = true)
@ConditionalOnClass(MongoClientSettings.Builder.class)
public class TraceMongoDbAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Conditional(EitherSynchronousOrReactiveContextProviderPresent.class)
	TraceMongoClientSettingsBuilderCustomizer traceMongoClientSettingsBuilderCustomizer(Tracer tracer,
			ObjectProvider<List<TraceMongoSpanCustomizer>> customizers) {
		return new TraceMongoClientSettingsBuilderCustomizer(tracer, customizers.getIfAvailable(ArrayList::new));
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("com.mongodb.client.SynchronousContextProvider")
	@ConditionalOnClass(name = "com.mongodb.reactivestreams.client.ReactiveContextProvider")
	TraceReactiveMongoClientSettingsBuilderCustomizer traceReactiveMongoClientSettingsBuilderCustomizer() {
		return new TraceReactiveMongoClientSettingsBuilderCustomizer();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnMissingClass("com.mongodb.reactivestreams.client.ReactiveContextProvider")
	@ConditionalOnClass(name = "com.mongodb.client.SynchronousContextProvider")
	TraceSynchronousMongoClientSettingsBuilderCustomizer traceSynchronousMongoClientSettingsBuilderCustomizer(
			Tracer tracer) {
		return new TraceSynchronousMongoClientSettingsBuilderCustomizer(tracer);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(name = { "com.mongodb.client.SynchronousContextProvider",
			"com.mongodb.reactivestreams.client.ReactiveContextProvider" })
	TraceAllTypesMongoClientSettingsBuilderCustomizer traceAllTypesMongoClientSettingsBuilderCustomizer(Tracer tracer) {
		return new TraceAllTypesMongoClientSettingsBuilderCustomizer(tracer);
	}

	static class MongoCustomizersConfiguration {

		@Bean
		TraceMongoSpanCustomizer traceMongoClusterIdSpanCustomizer() {
			return new TraceMongoClusterIdSpanCustomizer();
		}

		@Bean
		@ConditionalOnProperty("spring.sleuth.mongodb.socket-address-span-customizer.enabled")
		TraceMongoSpanCustomizer traceMongoSocketAddressSpanCustomizer() {
			return new TraceMongoSocketAddressSpanCustomizer();
		}

	}

	static class EitherSynchronousOrReactiveContextProviderPresent extends AnyNestedCondition {

		EitherSynchronousOrReactiveContextProviderPresent() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnClass(name = "com.mongodb.client.SynchronousContextProvider")
		static class OnSychronousContextProvider {

		}

		@ConditionalOnClass(name = "com.mongodb.reactivestreams.client.ReactiveContextProvider")
		static class OnReactiveContextProvider {

		}

	}

}
