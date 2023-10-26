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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.tx;

import org.springframework.beans.factory.BeanFactory;
import io.micrometer.tracing.instrument.tx.TraceReactiveTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

class ReactiveTransactionManagerInstrumenter
		extends AbstractTransactionManagerInstrumenter<ReactiveTransactionManager> {

	ReactiveTransactionManagerInstrumenter(BeanFactory beanFactory) {
		super(beanFactory, ReactiveTransactionManager.class);
	}

	@Override
	Class tracedClass() {
		return TraceReactiveTransactionManager.class;
	}

	@Override
	ReactiveTransactionManager wrap(ReactiveTransactionManager transactionManager) {
		return new TraceReactiveTransactionManager(transactionManager, this.beanFactory);
	}

}
