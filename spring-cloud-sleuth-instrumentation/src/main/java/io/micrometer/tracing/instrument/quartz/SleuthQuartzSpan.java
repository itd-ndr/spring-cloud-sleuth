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

package io.micrometer.tracing.instrument.quartz;

import io.micrometer.tracing.docs.DocumentedSpan;
import io.micrometer.tracing.docs.TagKey;

enum SleuthQuartzSpan implements DocumentedSpan {

	/**
	 * Span created when trigger is fired and then completed.
	 */
	QUARTZ_TRIGGER_SPAN {
		@Override
		public String getName() {
			return "%s";
		}

		@Override
		public TagKey[] getTagKeys() {
			return Tags.values();
		}
	};

	enum Tags implements TagKey {

		/**
		 * Name of the trigger.
		 */
		TRIGGER {
			@Override
			public String getKey() {
				return "quartz.trigger";
			}
		}

	}

}