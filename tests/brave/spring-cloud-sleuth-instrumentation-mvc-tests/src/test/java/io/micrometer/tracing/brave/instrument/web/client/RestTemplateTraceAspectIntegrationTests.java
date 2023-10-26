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

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import brave.Span;
import brave.Tracing;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.sampler.Sampler;
import brave.test.TestSpanHandler;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.instrument.web.mvc.TracingAsyncClientHttpRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.async.WebAsyncTask;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = RestTemplateTraceAspectIntegrationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.sleuth.web.client.skipPattern=/issue.*", "spring.sleuth.web.servlet.enabled=false"})
@DirtiesContext
class RestTemplateTraceAspectIntegrationTests {

	@Autowired
	WebApplicationContext context;

	@Autowired
	AspectTestingController controller;

	@Autowired
	CurrentTraceContext currentTraceContext;

	@Autowired
	Tracing tracer;

	@Autowired
	TestSpanHandler spans;

	@Autowired
	RestTemplate restTemplate;

	private MockMvc mockMvc;

	@BeforeEach
	void init() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context).build();
		this.controller.reset();
		this.spans.clear();
	}

	@BeforeEach
	@AfterEach
	void verify() {
		then(this.tracer.tracer().currentSpan()).isNull();
	}

	@Test
	void should_set_span_data_on_headers_via_aspect_in_synchronous_call() throws Exception {
		whenARequestIsSentToASyncEndpoint();

		thenTraceIdHasBeenSetOnARequestHeader();
		thenClientKindIsReported();
	}

	@Test
	void should_set_span_data_on_headers_when_sending_a_request_via_async_rest_template() throws Exception {
		whenARequestIsSentToAnAsyncRestTemplateEndpoint();

		thenTraceIdHasBeenSetOnARequestHeader();
		Awaitility.await().untilAsserted(this::thenClientKindIsReported);
	}

	@Test
	void should_set_span_data_on_headers_via_aspect_in_asynchronous_callable() throws Exception {
		whenARequestIsSentToAnAsyncEndpoint("/callablePing");

		thenTraceIdHasBeenSetOnARequestHeader();
		thenClientKindIsReported();
	}

	@Test
	void should_set_span_data_on_headers_via_aspect_in_asynchronous_web_async() throws Exception {
		whenARequestIsSentToAnAsyncEndpoint("/webAsyncTaskPing");

		thenTraceIdHasBeenSetOnARequestHeader();
		thenClientKindIsReported();
	}

	// issue #1047
	@Test
	void should_not_create_a_client_span_for_filtered_out_paths() throws Exception {
		whenARequestIsSentToASyncEndpointThatShouldBeFilteredOut();

		then(this.currentTraceContext.get()).isNull();
		then(this.spans).isEmpty();
	}

	private void whenARequestIsSentToAnAsyncRestTemplateEndpoint() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.get("/asyncRestTemplate").accept(MediaType.TEXT_PLAIN)).andReturn();
	}

	private void whenARequestIsSentToASyncEndpoint() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.get("/syncPing").accept(MediaType.TEXT_PLAIN)).andReturn();
	}

	private void whenARequestIsSentToASyncEndpointThatShouldBeFilteredOut() throws Exception {
		this.mockMvc.perform(MockMvcRequestBuilders.get("/issue1047_start").accept(MediaType.TEXT_PLAIN)).andReturn();
	}

	private void thenTraceIdHasBeenSetOnARequestHeader() {
		assertThat(this.controller.getTraceId()).matches("^(?!\\s*$).+");
	}

	// Brave was never designed to run tests of server and client in one test
	// that's why we have to pick only CLIENT side
	private void thenClientKindIsReported() {
		assertThat(this.spans.spans().stream().map(MutableSpan::kind).collect(Collectors.toList()))
				.contains(Span.Kind.CLIENT);
	}

	private void whenARequestIsSentToAnAsyncEndpoint(String url) throws Exception {
		MvcResult mvcResult = this.mockMvc.perform(MockMvcRequestBuilders.get(url).accept(MediaType.TEXT_PLAIN))
				.andExpect(request().asyncStarted()).andReturn();
		mvcResult.getAsyncResult(SECONDS.toMillis(2));
		this.mockMvc.perform(asyncDispatch(mvcResult)).andDo(print()).andExpect(status().isOk());
	}

	@EnableAutoConfiguration
	@Import(AspectTestingController.class)
	public static class Config {

		@Bean
		public RestTemplate restTemplate() {
			return new RestTemplate();
		}

		@Bean
		Sampler alwaysSampler() {
			return Sampler.ALWAYS_SAMPLE;
		}

		@Bean
		public AsyncRestTemplate asyncRestTemplate(
				io.micrometer.tracing.CurrentTraceContext currentTraceContext,
				HttpClientHandler httpClientHandler) {
			AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate();
			asyncRestTemplate.setInterceptors(Collections.singletonList(
					TracingAsyncClientHttpRequestInterceptor.create(currentTraceContext, httpClientHandler)));
			return asyncRestTemplate;
		}

		@Bean
		SpanHandler testSpanHandler() {
			return new TestSpanHandler();
		}

	}

	@RestController
	public static class AspectTestingController {

		@Autowired
		Tracing tracer;

		@Autowired
		RestTemplate restTemplate;

		@Autowired
		Environment environment;

		@Autowired
		AsyncRestTemplate asyncRestTemplate;

		private String traceId;

		public void reset() {
			this.traceId = null;
		}

		@GetMapping(value = "/issue1047_end", produces = MediaType.TEXT_PLAIN_VALUE)
		public String issue1047() {
			return "should_filter_out_this_endpoint";
		}

		@GetMapping(value = "/issue1047_start", produces = MediaType.TEXT_PLAIN_VALUE)
		public String issue1047Start() {
			return callAndReturnIssue1047();
		}

		@GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
		public String home(@RequestHeader(value = "X-B3-SpanId", required = false) String traceId) {
			this.traceId = traceId == null ? "UNKNOWN" : traceId;
			return "trace=" + this.getTraceId();
		}

		@GetMapping(value = "/customTag", produces = MediaType.TEXT_PLAIN_VALUE)
		public String customTag(@RequestHeader(value = "X-B3-TraceId", required = false) String traceId) {
			this.traceId = traceId == null ? "UNKNOWN" : traceId;
			return "trace=" + this.getTraceId();
		}

		@GetMapping(value = "/asyncRestTemplate", produces = MediaType.TEXT_PLAIN_VALUE)
		public String asyncRestTemplate() throws ExecutionException, InterruptedException {
			return callViaAsyncRestTemplateAndReturnOk();
		}

		@GetMapping(value = "/syncPing", produces = MediaType.TEXT_PLAIN_VALUE)
		public String syncPing() {
			return callAndReturnOk();
		}

		@GetMapping(value = "/callablePing", produces = MediaType.TEXT_PLAIN_VALUE)
		public Callable<String> asyncPing() {
			return new Callable<String>() {
				@Override
				public String call() throws Exception {
					return callAndReturnOk();
				}
			};
		}

		@GetMapping(value = "/webAsyncTaskPing", produces = MediaType.TEXT_PLAIN_VALUE)
		public WebAsyncTask<String> webAsyncTaskPing() {
			return new WebAsyncTask<>(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return callAndReturnOk();
				}
			});
		}

		private String callAndReturnIssue1047() {
			this.restTemplate.getForObject("http://localhost:" + port() + "/issue1047_end", String.class);
			return "OK";
		}

		private String callAndReturnOk() {
			this.restTemplate.getForObject("http://localhost:" + port(), String.class);
			return "OK";
		}

		private String callViaAsyncRestTemplateAndReturnOk() throws ExecutionException, InterruptedException {
			this.asyncRestTemplate.getForEntity("http://localhost:" + port(), String.class).get();
			return "OK";
		}

		private int port() {
			return this.environment.getProperty("local.server.port", Integer.class);
		}

		String getTraceId() {
			return this.traceId;
		}

	}

}
