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

package io.micrometer.tracing;

import com.mongodb.client.MongoClient;
import org.mockito.BDDMockito;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * @author Marcin Grzejszczak
 */
@AutoConfiguration
class SleuthTestAutoConfiguration {

	static class TestMongoConfiguration {

		@Bean
		@Primary
		@ConditionalOnProperty(value = "test.mongo.mock.enabled", matchIfMissing = true)
		MongoClient testMongoClient() {
			return BDDMockito.mock(MongoClient.class);
		}

		@Bean(name = "mongoHealthIndicator")
		@Primary
		HealthIndicator testMongoHealthIndicator() {
			return () -> Health.up().build();
		}

	}

	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	static class ReactiveConfiguration {

		@Import(PermitAllWebFluxSecurityConfiguration.class)
		static class ImportConfiguration {

		}

	}

	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static class ServletConfiguration {

		@Import(PermitAllServletConfiguration.class)
		static class ImportConfiguration {

		}

	}

}
