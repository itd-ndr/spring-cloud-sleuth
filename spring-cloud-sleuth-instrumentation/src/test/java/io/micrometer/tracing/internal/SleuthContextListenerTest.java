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

package io.micrometer.tracing.internal;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class SleuthContextListenerTest {

	@Test
	void should_be_usable_using_context() {
		new ApplicationContextRunner().withUserConfiguration(SleuthContextListener.class).run(context -> {
			SleuthContextListener listener = SleuthContextListener.getBean(context);

			BDDAssertions.then(listener).isNotNull();
			BDDAssertions.then(listener.isUnusable()).isFalse();
		});
	}

	@Test
	void should_be_usable_using_beanfactory() {
		new ApplicationContextRunner().withUserConfiguration(SleuthContextListener.class).run(context -> {
			SleuthContextListener listener = SleuthContextListener.getBean(context.getBeanFactory());

			BDDAssertions.then(listener).isNotNull();
			BDDAssertions.then(listener.isUnusable()).isFalse();
		});
	}

}
