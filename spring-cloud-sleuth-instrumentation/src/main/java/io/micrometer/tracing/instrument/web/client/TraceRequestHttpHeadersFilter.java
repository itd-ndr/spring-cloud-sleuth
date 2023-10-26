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

package io.micrometer.tracing.instrument.web.client;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.http.HttpClientRequest;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

/**
 * Trace representation of {@link HttpHeadersFilter} for a request.
 *
 * @author Marcin Grzejszczak
 * @since 3.0.0
 */
public class TraceRequestHttpHeadersFilter extends AbstractHttpHeadersFilter {

	private static final Log log = LogFactory.getLog(TraceRequestHttpHeadersFilter.class);

	static final String TRACE_REQUEST_ATTR = TraceContext.class.getName();

	static final String TRACE_REQUEST_ATTR_FROM_TRACE_WEB_FILTER = Span.class.getName();

	public TraceRequestHttpHeadersFilter(Tracer tracer, HttpClientHandler handler, Propagator propagator) {
		super(tracer, handler, propagator);
	}

	@Override
	public HttpHeaders filter(HttpHeaders input, ServerWebExchange exchange) {
		if (log.isDebugEnabled()) {
			log.debug("Will instrument the HTTP request headers [" + exchange.getRequest().getHeaders() + "]");
		}
		ServerHttpClientRequest request = new ServerHttpClientRequest(exchange.getRequest(), input);
		Span currentSpan = currentSpan(exchange, request);
		Span span = injectedSpan(request, currentSpan);
		if (log.isDebugEnabled()) {
			log.debug("Client span  " + span + " created for the request. New headers are "
					+ request.filteredHeaders.toSingleValueMap());
		}
		exchange.getAttributes().put(SPAN_ATTRIBUTE, span);
		HttpHeaders headersWithInput = new HttpHeaders();
		headersWithInput.addAll(input);
		addHeadersWithInput(request.filteredHeaders, headersWithInput);
		if (headersWithInput.containsKey("b3") || headersWithInput.containsKey("B3")) {
			headersWithInput.keySet().remove("b3");
			headersWithInput.keySet().remove("B3");
		}
		return headersWithInput;
	}

	private Span currentSpan(ServerWebExchange exchange, ServerHttpClientRequest request) {
		Span currentSpan = currentSpan(exchange);
		if (currentSpan != null) {
			return currentSpan;
		}
		// Usually, an HTTP client would not attempt to resume a trace from headers, as a
		// server would always place its span in scope. However, in commit 848442e,
		// this behavior was added in support of gateway.
		return this.propagator.extract(request, HttpClientRequest::header).start();
	}

	private Span currentSpan(ServerWebExchange exchange) {
		Object attribute = exchange.getAttribute(TRACE_REQUEST_ATTR);
		Object span = exchange.getAttribute(TRACE_REQUEST_ATTR_FROM_TRACE_WEB_FILTER);
		if (attribute instanceof Span span1) {
			if (log.isDebugEnabled()) {
				log.debug("Found trace request attribute in the server web exchange [" + attribute + "]");
			}
			return span1;
		}
		else if (span instanceof Span span1) {
			if (log.isDebugEnabled()) {
				log.debug("Found trace request attribute in the server web exchange set by TraceWebFilter [" + span
						+ "]");
			}
			return span1;
		}
		return this.tracer.currentSpan();
	}

	private Span injectedSpan(ServerHttpClientRequest request, Span currentSpan) {
		if (currentSpan == null) {
			return this.handler.handleSend(request);
		}
		return this.handler.handleSend(request, currentSpan.context());
	}

	private void addHeadersWithInput(HttpHeaders filteredHeaders, HttpHeaders headersWithInput) {
		for (Map.Entry<String, List<String>> entry : filteredHeaders.entrySet()) {
			String key = entry.getKey();
			List<String> value = entry.getValue();
			headersWithInput.put(key, value);
		}
	}

	@Override
	public boolean supports(Type type) {
		return type.equals(Type.REQUEST);
	}

}
