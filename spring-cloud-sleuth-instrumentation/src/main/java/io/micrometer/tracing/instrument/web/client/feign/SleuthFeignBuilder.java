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

package io.micrometer.tracing.instrument.web.client.feign;

import feign.Client;
import feign.Feign;
import feign.Retryer;

import org.springframework.beans.factory.BeanFactory;

/**
 * Contains {@link Feign.Builder} implementation with tracing components that close spans
 * on completion of request processing.
 *
 * @author Marcin Grzejszczak
 * @since 1.0.0
 */
public final class SleuthFeignBuilder {

	private SleuthFeignBuilder() {
	}

	public static Feign.Builder builder(BeanFactory beanFactory) {
		return builder(beanFactory, null);
	}

	public static Feign.Builder builder(BeanFactory beanFactory, Client delegate) {
		return Feign.builder().retryer(Retryer.NEVER_RETRY).client(client(beanFactory, delegate));
	}

	private static Client client(BeanFactory beanFactory, Client delegate) {
		if (delegate == null) {
			return new LazyClient(beanFactory);
		}
		else {
			return new LazyClient(beanFactory, delegate);
		}
	}

}
