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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

/**
 * Wraps all tracing related components into test representations. That way additional
 * assertions can take place.
 */
public class TestTracingBeanPostProcessor implements BeanPostProcessor {

	TestTracer testTracer;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof Tracer tracer && !(bean instanceof TestTracer)) {
			this.testTracer = new TestTracer(tracer);
			return this.testTracer;
		}
		else if (bean instanceof Propagator propagator && !(bean instanceof TestPropagator)) {
			return new TestPropagator(propagator, this.testTracer);
		}
		return bean;
	}

}
