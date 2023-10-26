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

package io.micrometer.tracing.brave.instrument.redis;

import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Chao Chang
 */
@SpringBootTest(classes = BraveRedisAutoConfigurationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = { "spring.sleuth.redis.enabled=true",
				"spring.sleuth.redis.legacy.enabled=true", "spring.sleuth.redis.remote-service-name=redis-foo" })
class BraveRedisAutoConfigurationTests {

	@Autowired
	ClientResources clientResources;

	@Autowired
	TraceLettuceClientResourcesBuilderCustomizer customizer;

	@Test
	void tracing_should_be_set() {
		then(this.clientResources.tracing().isEnabled()).isTrue();
		then(this.customizer).isNotNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	protected static class Config {

	}

}
