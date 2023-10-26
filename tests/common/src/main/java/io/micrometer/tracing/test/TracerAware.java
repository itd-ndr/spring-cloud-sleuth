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

package io.micrometer.tracing.test;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.http.HttpClientHandler;
import io.micrometer.tracing.http.HttpRequestParser;
import io.micrometer.tracing.http.HttpServerHandler;
import io.micrometer.tracing.propagation.Propagator;

public interface TracerAware {

	Tracer tracer();

	TracerAware sampler(TraceSampler sampler);

	CurrentTraceContext currentTraceContext();

	Propagator propagator();

	HttpServerHandler httpServerHandler();

	TracerAware clientRequestParser(HttpRequestParser httpRequestParser);

	HttpClientHandler httpClientHandler();

	enum TraceSampler {

		ON, OFF

	}

}