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

package io.micrometer.tracing.instrument.jdbc;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.actuate.autoconfigure.tracing.instrument.jdbc.TraceJdbcAutoConfiguration;

public abstract class TraceQueryExecutionListenerTests extends TraceListenerStrategyTests {

	@Override
	ApplicationContextRunner parentContextRunner() {
		return new ApplicationContextRunner()
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class, TraceJdbcAutoConfiguration.class,
								autoConfiguration(), PropertyPlaceholderAutoConfiguration.class))
				.withUserConfiguration(testConfiguration())
				.withPropertyValues("spring.datasource.initialization-mode=never",
						"spring.datasource.url:jdbc:h2:mem:testdb-baz", "spring.datasource.hikari.pool-name=test")
				.withClassLoader(new FilteredClassLoader("com.p6spy"));
	}

}
