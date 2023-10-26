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

import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.brave.bridge.BraveAccessor;

public class HttpClientBeanPostProcessorTest
		extends io.micrometer.tracing.instrument.web.client.HttpClientBeanPostProcessorTest {

	@Override
	public TraceContext traceContext() {
		return BraveAccessor
				.traceContext(brave.propagation.TraceContext.newBuilder().traceId(1).spanId(2).sampled(true).build());
	}

}