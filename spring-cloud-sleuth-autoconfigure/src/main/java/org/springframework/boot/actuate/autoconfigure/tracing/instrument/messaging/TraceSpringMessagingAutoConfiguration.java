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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.messaging;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.micrometer.tracing.SpanNamer;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.instrument.messaging.MessageHeaderPropagatorGetter;
import io.micrometer.tracing.instrument.messaging.MessageHeaderPropagatorSetter;
import io.micrometer.tracing.instrument.messaging.TraceMessagingAspect;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.support.MessageHeaderAccessor;

@AutoConfiguration
@ConditionalOnClass(MessageHeaderAccessor.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(value = "spring.sleuth.messaging.enabled", matchIfMissing = true)
@EnableConfigurationProperties({SleuthIntegrationMessagingProperties.class, SleuthMessagingProperties.class})
class TraceSpringMessagingAutoConfiguration {

	@Bean
	@ConditionalOnProperty(value = "spring.sleuth.messaging.aspect.enabled", matchIfMissing = true)
	TraceMessagingAspect traceMessagingAspect(Tracer tracer, SpanNamer spanNamer) {
		return new TraceMessagingAspect(tracer, spanNamer);
	}

	@Bean
	@ConditionalOnMissingBean(value = MessageHeaderAccessor.class, parameterizedContainer = Propagator.Setter.class)
	Propagator.Setter<MessageHeaderAccessor> traceMessagePropagationSetter() {
		return new MessageHeaderPropagatorSetter();
	}

	@Bean
	@ConditionalOnMissingBean(value = MessageHeaderAccessor.class, parameterizedContainer = Propagator.Getter.class)
	Propagator.Getter<MessageHeaderAccessor> traceMessagePropagationGetter() {
		return new MessageHeaderPropagatorGetter();
	}

}
