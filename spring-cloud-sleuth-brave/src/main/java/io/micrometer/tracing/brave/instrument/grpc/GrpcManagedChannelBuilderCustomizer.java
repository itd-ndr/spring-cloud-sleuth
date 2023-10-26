/*
 * Copyright 2018-2021 the original author or authors.
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

package io.micrometer.tracing.brave.instrument.grpc;

import io.grpc.ManagedChannelBuilder;

/**
 * Callback interface that can be implemented by beans wishing to further customize the
 * {@link io.grpc.ManagedChannelBuilder} via the {@link SpringAwareManagedChannelBuilder}.
 *
 * @author Tyler Van Gorder
 * @since 3.0.0
 */
public interface GrpcManagedChannelBuilderCustomizer {

	/**
	 * Customizes the channel builder.
	 * @param managedChannelBuilder the builder to customize
	 */
	void customize(ManagedChannelBuilder<?> managedChannelBuilder);

}