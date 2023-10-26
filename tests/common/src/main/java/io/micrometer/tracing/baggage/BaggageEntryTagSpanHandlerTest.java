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

package io.micrometer.tracing.baggage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import io.micrometer.tracing.BaggageInScope;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.TestSpanHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Taras Danylchuk
 */
@ContextConfiguration(classes = BaggageEntryTagSpanHandlerTest.TestConfig.class)
@TestPropertySource(properties = { "spring.sleuth.baggage.remote-fields[0]=country-code",
		"spring.sleuth.baggage.remote-fields[1]=x-vcap-request-id",
		"spring.sleuth.baggage.tag-fields[0]=country-code" })
public abstract class BaggageEntryTagSpanHandlerTest {

	BaggageInScope countryCode;

	BaggageInScope requestId;

	@Autowired
	private Tracer tracer;

	@Autowired
	private TestSpanHandler spans;

	private ScopedSpan span;

	@BeforeEach
	public void setUp() {
		this.spans.clear();
		this.span = this.tracer.startScopedSpan("my-scoped-span");
		this.countryCode = this.tracer.createBaggage("country-code");
		this.countryCode.set("FO");
		this.requestId = this.tracer.createBaggage("x-vcap-request-id");
		this.requestId.set("f4308d05-2228-4468-80f6-92a8377ba193");
	}

	@Test
	public void shouldReportWithBaggageInTags() {
		this.span.end();

		Assertions.assertThat(this.spans).hasSize(1);
		// REQUEST_ID is NOT in the tag fields
		Assertions.assertThat(this.spans.get(0).getTags()).hasSize(1).containsEntry(countryCode.name(), "FO");
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	static class TestConfig {

	}

}
