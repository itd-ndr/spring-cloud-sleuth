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

package io.micrometer.tracing.brave.instrument.web.client;

import brave.sampler.Sampler;

import org.springframework.boot.test.context.SpringBootTest;
import io.micrometer.tracing.brave.BraveTestSpanHandler;
import io.micrometer.tracing.test.TestSpanHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = WebClientDiscoveryExceptionTests.Config.class)
public class WebClientDiscoveryExceptionTests extends
		io.micrometer.tracing.instrument.web.client.discoveryexception.WebClientDiscoveryExceptionTests {

	@Configuration(proxyBeanMethods = false)
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
