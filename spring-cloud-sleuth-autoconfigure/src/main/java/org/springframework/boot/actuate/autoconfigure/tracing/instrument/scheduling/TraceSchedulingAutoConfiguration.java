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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.scheduling;

import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.actuate.autoconfigure.tracing.brave.BraveAutoConfiguration;
import io.micrometer.tracing.instrument.scheduling.TraceSchedulingAspect;
import org.springframework.context.annotation.Bean;

/**
 * Registers beans related to task scheduling.
 *
 * @author Michal Chmielarz, 4financeIT
 * @author Spencer Gibb
 * @since 1.0.0
 * @see TraceSchedulingAspect
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.ProceedingJoinPoint")
@ConditionalOnProperty(value = "spring.sleuth.scheduled.enabled", matchIfMissing = true)
@ConditionalOnBean(Tracer.class)
@EnableConfigurationProperties(SleuthSchedulingProperties.class)
@AutoConfigureAfter(BraveAutoConfiguration.class)
public class TraceSchedulingAutoConfiguration {

	@Bean
	TraceSchedulingAspect traceSchedulingAspect(Tracer tracer, SleuthSchedulingProperties sleuthSchedulingProperties) {
		String skipPatternString = sleuthSchedulingProperties.getSkipPattern();
		Pattern skipPattern = skipPatternString != null ? Pattern.compile(skipPatternString) : null;
		return new TraceSchedulingAspect(tracer, skipPattern);
	}

}
