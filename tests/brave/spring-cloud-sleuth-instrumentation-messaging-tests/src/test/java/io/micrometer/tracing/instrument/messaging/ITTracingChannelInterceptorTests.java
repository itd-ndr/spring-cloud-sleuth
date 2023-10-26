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

package io.micrometer.tracing.instrument.messaging;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.propagation.StrictCurrentTraceContext;
import brave.test.TestSpanHandler;
import jakarta.annotation.PreDestroy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Ported from
 * org.springframework.cloud.sleuth.instrument.messaging.TraceChannelInterceptorTest to
 * allow sleuth to decommission its implementation.
 *
 * @author Marcin Grzejszczak
 */
@SpringBootTest(classes = ITTracingChannelInterceptorTests.App.class, webEnvironment = WebEnvironment.NONE,
		properties = "spring.sleuth.integration.enabled=true")
@DirtiesContext
public class ITTracingChannelInterceptorTests implements MessageHandler {

	@Autowired
	@Qualifier("directChannel")
	DirectChannel directChannel;

	@Autowired
	@Qualifier("executorChannel")
	ExecutorChannel executorChannel;

	@Autowired
	Tracer tracer;

	@Autowired
	TestSpanHandler spans;

	Message<?> message;

	Span currentSpan;

	@Override
	public void handleMessage(Message<?> msg) {
		this.message = msg;
		this.currentSpan = this.tracer.currentSpan();
		if (this.message.getHeaders().containsKey("THROW_EXCEPTION")) {
			throw new RuntimeException("A terrible exception has occurred");
		}
	}

	@BeforeEach
	void init() {
		this.directChannel.subscribe(this);
		this.executorChannel.subscribe(this);
	}

	@AfterEach
	void close() {
		this.directChannel.unsubscribe(this);
		this.executorChannel.unsubscribe(this);
	}

	// formerly known as TraceChannelInterceptorTest.executableSpanCreation
	@Test
	void propagatesNoopSpan() {
		this.directChannel.send(MessageBuilder.withPayload("hi").setHeader("b3", "0").build());

		assertThat(this.message.getHeaders()).containsEntry("b3", "0");

		assertThat(this.currentSpan.isNoop()).isTrue();
	}

	@Test
	void messageHeadersStillMutableForStomp() {
		this.directChannel.send(MessageBuilder.withPayload("hi").setHeader("stompCommand", "DISCONNECT").build());

		assertThat(MessageHeaderAccessor.getAccessor(this.message, MessageHeaderAccessor.class)).isNotNull();

		this.message = null;
		this.directChannel.send(MessageBuilder.withPayload("hi").setHeader("simpMessageType", "sth").build());

		assertThat(MessageHeaderAccessor.getAccessor(this.message, MessageHeaderAccessor.class)).isNotNull();
	}

	@Test
	void messageHeadersImmutableForNonStomp() {
		this.directChannel.send(MessageBuilder.withPayload("hi").setHeader("foo", "bar").build());

		assertThat(MessageHeaderAccessor.getAccessor(this.message, MessageHeaderAccessor.class)).isNull();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	static class App {

		ExecutorService service = Executors.newSingleThreadExecutor();

		@Bean
		TestSpanHandler testSpanHandler() {
			return new TestSpanHandler();
		}

		@Bean
		StrictCurrentTraceContext currentTraceContext() {
			return StrictCurrentTraceContext.create();
		}

		@Bean
		Tracing tracing(StrictCurrentTraceContext currentTraceContext, TestSpanHandler spanHandler) {
			return Tracing.newBuilder().currentTraceContext(currentTraceContext).addSpanHandler(spanHandler).build();
		}

		@Bean
		Tracer tracer(Tracing tracing) {
			return tracing.tracer();
		}

		@Bean
		ExecutorChannel executorChannel() {
			return new ExecutorChannel(this.service);
		}

		@PreDestroy
		public void destroy() {
			this.service.shutdown();
		}

		@Bean
		DirectChannel directChannel() {
			return new DirectChannel();
		}

		@Bean
		public MessagingTemplate messagingTemplate(DirectChannel directChannel) {
			return new MessagingTemplate(directChannel);
		}

	}

}
