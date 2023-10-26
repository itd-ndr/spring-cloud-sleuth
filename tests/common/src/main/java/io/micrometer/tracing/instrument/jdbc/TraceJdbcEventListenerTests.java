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

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.actuate.autoconfigure.tracing.instrument.jdbc.TraceJdbcAutoConfiguration;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.TestSpanHandler;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class TraceJdbcEventListenerTests extends TraceListenerStrategyTests {

	protected final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TraceJdbcAutoConfiguration.class, autoConfiguration(), PropertyPlaceholderAutoConfiguration.class))
			.withUserConfiguration(testConfiguration())
			.withPropertyValues("spring.datasource.initialization-mode=never",
					"spring.datasource.url=jdbc:h2:mem:testdb-baz", "spring.datasource.hikari.pool-name=test")
			.withClassLoader(new FilteredClassLoader("net.ttddyy.dsproxy"));

	@Override
	ApplicationContextRunner parentContextRunner() {
		return this.contextRunner;
	}

	@Test
	void testShouldUsePlaceholderInSqlTagOfSpansForPreparedStatementIfIncludeParameterValuesIsSetToFalse() {
		contextRunner.run(context -> {
			DataSource dataSource = context.getBean(DataSource.class);
			TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

			Connection connection = dataSource.getConnection();
			PreparedStatement preparedStatement = connection
					.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
			preparedStatement.setString(1, "");
			preparedStatement.setInt(2, 1);
			preparedStatement.executeUpdate();
			connection.close();

			assertThat(spanReporter.reportedSpans()).hasSize(2);
			FinishedSpan connectionSpan = spanReporter.reportedSpans().get(1);
			FinishedSpan statementSpan = spanReporter.reportedSpans().get(0);
			assertThat(connectionSpan.getName()).isEqualTo("connection");
			assertThat(statementSpan.getName()).isEqualTo("update");
			assertThat(statementSpan.getTags()).containsEntry(SPAN_SQL_QUERY_TAG_NAME,
					"UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
			assertThat(statementSpan.getTags()).containsEntry(SPAN_ROW_COUNT_TAG_NAME, "0");
		});
	}

	@Test
	void testShouldNotUsePlaceholderInSqlTagOfSpansForPreparedStatementIfIncludeParameterValuesIsSetToTrue() {
		contextRunner.withPropertyValues("spring.sleuth.jdbc.p6spy.tracing.include-parameter-values=true")
				.run(context -> {
					DataSource dataSource = context.getBean(DataSource.class);
					TestSpanHandler spanReporter = context.getBean(TestSpanHandler.class);

					Connection connection = dataSource.getConnection();
					PreparedStatement preparedStatement = connection
							.prepareStatement("UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = ? WHERE 0 = ?");
					preparedStatement.setString(1, "");
					preparedStatement.setInt(2, 1);
					preparedStatement.executeUpdate();
					connection.close();

					assertThat(spanReporter.reportedSpans()).hasSize(2);
					FinishedSpan connectionSpan = spanReporter.reportedSpans().get(1);
					FinishedSpan statementSpan = spanReporter.reportedSpans().get(0);
					assertThat(connectionSpan.getName()).isEqualTo("connection");
					assertThat(statementSpan.getName()).isEqualTo("update");
					assertThat(statementSpan.getTags()).containsEntry(SPAN_SQL_QUERY_TAG_NAME,
							"UPDATE INFORMATION_SCHEMA.TABLES SET table_Name = '' WHERE 0 = 1");
					assertThat(statementSpan.getTags()).containsEntry(SPAN_ROW_COUNT_TAG_NAME, "0");
				});
	}

}
