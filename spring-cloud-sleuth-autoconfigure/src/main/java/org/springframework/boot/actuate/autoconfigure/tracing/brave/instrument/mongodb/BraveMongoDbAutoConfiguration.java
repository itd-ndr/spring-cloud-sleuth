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

package org.springframework.boot.actuate.autoconfigure.tracing.brave.instrument.mongodb;

import brave.Tracing;
import brave.mongodb.MongoDBTracing;
import com.mongodb.MongoClientSettings;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.instrument.mongodb.TraceMongoDbAutoConfiguration;
import io.micrometer.tracing.brave.instrument.mongodb.TraceMongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables MongoDb span information propagation.
 *
 * Will only be applied if for some reason the main {@link TraceMongoDbAutoConfiguration}
 * will not be applied.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 * @deprecated use {@link TraceMongoDbAutoConfiguration}
 */
@AutoConfiguration
@ConditionalOnMissingClass("com.mongodb.reactivestreams.client.MongoClient")
@ConditionalOnBean(Tracing.class)
@AutoConfigureAfter({BraveAutoConfiguration.class, TraceMongoDbAutoConfiguration.class})
@AutoConfigureBefore(MongoAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.sleuth.mongodb.enabled", matchIfMissing = true)
@ConditionalOnClass({MongoClientSettings.Builder.class, MongoDBTracing.class})
@Deprecated
public class BraveMongoDbAutoConfiguration {

	@Bean
	// for tests
	@ConditionalOnMissingBean({ TraceMongoClientSettingsBuilderCustomizer.class,
			io.micrometer.tracing.instrument.mongodb.TraceMongoClientSettingsBuilderCustomizer.class })
	MongoClientSettingsBuilderCustomizer braveTraceMongoClientSettingsBuilderCustomizer(Tracing tracing) {
		return new TraceMongoClientSettingsBuilderCustomizer(tracing);
	}

}
