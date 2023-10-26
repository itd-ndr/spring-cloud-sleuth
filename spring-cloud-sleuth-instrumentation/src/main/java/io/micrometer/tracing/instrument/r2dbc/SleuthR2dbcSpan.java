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

package io.micrometer.tracing.instrument.r2dbc;

import io.micrometer.tracing.docs.DocumentedSpan;
import io.micrometer.tracing.docs.EventValue;
import io.micrometer.tracing.docs.TagKey;

enum SleuthR2dbcSpan implements DocumentedSpan {

	/**
	 * Span created on the Kafka consumer side.
	 */
	R2DBC_QUERY_SPAN {
		@Override
		public String getName() {
			return "query";
		}

		@Override
		public TagKey[] getTagKeys() {
			return Tags.values();
		}

		@Override
		public String prefix() {
			return "r2dbc.";
		}
	};

	enum Tags implements TagKey {

		/**
		 * Name of the R2DBC connection.
		 */
		CONNECTION {
			@Override
			public String getKey() {
				return "r2dbc.connection";
			}
		},

		/**
		 * Name of the R2DBC thread.
		 */
		THREAD {
			@Override
			public String getKey() {
				return "r2dbc.thread";
			}
		},

		/**
		 * Name of the R2DBC query.
		 */
		QUERY {
			@Override
			public String getKey() {
				return "r2dbc.query[%s]";
			}
		},

	}

	enum Events implements EventValue {

		/**
		 * Annotated before executing a method annotated with @ContinueSpan or @NewSpan.
		 */
		QUERY_RESULT {
			@Override
			public String getValue() {
				return "r2dbc.query_result";
			}
		}

	}

}
