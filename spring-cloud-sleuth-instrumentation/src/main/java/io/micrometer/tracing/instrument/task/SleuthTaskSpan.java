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

package io.micrometer.tracing.instrument.task;

import io.micrometer.tracing.docs.DocumentedSpan;

enum SleuthTaskSpan implements DocumentedSpan {

	/**
	 * Span created when a task runner is executed.
	 */
	TASK_RUNNER_SPAN {
		@Override
		public String getName() {
			return "%s";
		}
	},

	/**
	 * Span created within the lifecycle of a task.
	 */
	TASK_EXECUTION_LISTENER_SPAN {
		@Override
		public String getName() {
			return "%s";
		}
	}

}