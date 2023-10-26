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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.session;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import io.micrometer.tracing.instrument.session.TraceSessionRepositoryAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.session.SessionRepository;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} that registers instrumentation for Spring Session.
 *
 * @author Marcin Grzejszczak
 * @since 3.1.0
 */
@AutoConfiguration
@ConditionalOnClass(SessionRepository.class)
@ConditionalOnProperty(value = "spring.sleuth.session.enabled", matchIfMissing = true)
@ConditionalOnBean(Tracer.class)
@AutoConfigureAfter(BraveAutoConfiguration.class)
public class TraceSessionAutoConfiguration {

	// Bean post processors don't work cause session-redis requires concrete classes
	@Bean
	TraceSessionRepositoryAspect traceSessionRepositoryAspect(Tracer tracer, CurrentTraceContext currentTraceContext) {
		return new TraceSessionRepositoryAspect(tracer, currentTraceContext);
	}

}