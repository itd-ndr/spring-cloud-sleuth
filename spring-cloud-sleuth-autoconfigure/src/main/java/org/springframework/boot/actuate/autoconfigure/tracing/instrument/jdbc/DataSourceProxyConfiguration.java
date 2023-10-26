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

package org.springframework.boot.actuate.autoconfigure.tracing.instrument.jdbc;

import java.util.ArrayList;
import java.util.List;

import javax.sql.CommonDataSource;

import net.ttddyy.dsproxy.listener.MethodExecutionListener;
import net.ttddyy.dsproxy.listener.QueryCountStrategy;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.proxy.GlobalConnectionIdManager;
import net.ttddyy.dsproxy.proxy.ResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.proxy.SimpleResultSetProxyLogicFactory;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.transform.ParameterTransformer;
import net.ttddyy.dsproxy.transform.QueryTransformer;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import io.micrometer.tracing.instrument.jdbc.DataSourceNameResolver;
import io.micrometer.tracing.instrument.jdbc.DataSourceProxyBuilderCustomizer;
import io.micrometer.tracing.instrument.jdbc.DataSourceProxyConnectionIdManagerProvider;
import io.micrometer.tracing.instrument.jdbc.DataSourceProxyDataSourceDecorator;
import io.micrometer.tracing.instrument.jdbc.DataSourceProxyProperties;
import io.micrometer.tracing.instrument.jdbc.TraceListenerStrategySpanCustomizer;
import io.micrometer.tracing.instrument.jdbc.TraceQueryExecutionListener;
import org.springframework.context.annotation.Bean;

/**
 * Configuration for integration with datasource-proxy, allows to use define custom
 * {@link QueryExecutionListener}, {@link ParameterTransformer} and
 * {@link QueryTransformer}.
 *
 * @author Arthur Gavlyukovskiy
 * @author Chintan Radia
 */
@ConditionalOnClass(ProxyDataSource.class)
@ConditionalOnProperty(name = "spring.sleuth.jdbc.datasource-proxy.enabled", havingValue = "true",
		matchIfMissing = true)
class DataSourceProxyConfiguration {

	@Bean
	@ConditionalOnMissingBean
	DataSourceProxyConnectionIdManagerProvider traceConnectionIdManagerProvider() {
		return GlobalConnectionIdManager::new;
	}

	@Bean
	DataSourceProxyBuilderCustomizer proxyDataSourceBuilderConfigurer(
			ObjectProvider<QueryCountStrategy> queryCountStrategy,
			ObjectProvider<List<QueryExecutionListener>> listeners,
			ObjectProvider<List<MethodExecutionListener>> methodExecutionListeners,
			ObjectProvider<ParameterTransformer> parameterTransformer,
			ObjectProvider<QueryTransformer> queryTransformer,
			ObjectProvider<ResultSetProxyLogicFactory> resultSetProxyLogicFactory,
			ObjectProvider<DataSourceProxyConnectionIdManagerProvider> dataSourceProxyConnectionIdManagerProvider,
			TraceJdbcProperties traceJdbcProperties) {
		return new DataSourceProxyBuilderCustomizer(queryCountStrategy.getIfAvailable(() -> null),
				listeners.getIfAvailable(() -> null), methodExecutionListeners.getIfAvailable(() -> null),
				parameterTransformer.getIfAvailable(() -> null), queryTransformer.getIfAvailable(() -> null),
				resultSetProxyLogicFactory.getIfAvailable(() -> null),
				dataSourceProxyConnectionIdManagerProvider.getIfAvailable(() -> null), props(traceJdbcProperties));
	}

	private DataSourceProxyProperties props(TraceJdbcProperties traceJdbcProperties) {
		TraceJdbcProperties.DataSourceProxyProperties originalProxy = traceJdbcProperties.getDatasourceProxy();
		DataSourceProxyProperties props = new DataSourceProxyProperties();
		BeanUtils.copyProperties(originalProxy, props);
		props.setLogging(DataSourceProxyProperties.DataSourceProxyLogging.valueOf(originalProxy.getLogging().name()));
		BeanUtils.copyProperties(originalProxy.getQuery(), props.getQuery());
		BeanUtils.copyProperties(originalProxy.getSlowQuery(), props.getSlowQuery());
		return props;
	}

	@Bean
	@DependsOnDatabaseInitialization
	DataSourceProxyDataSourceDecorator proxyDataSourceDecorator(
			DataSourceProxyBuilderCustomizer dataSourceProxyBuilderCustomizer,
			DataSourceNameResolver dataSourceNameResolver) {
		return new DataSourceProxyDataSourceDecorator(dataSourceProxyBuilderCustomizer, dataSourceNameResolver);
	}

	@Bean
	TraceQueryExecutionListener traceQueryExecutionListener(BeanFactory beanFactory,
			TraceJdbcProperties dataSourceDecoratorProperties,
			ObjectProvider<List<TraceListenerStrategySpanCustomizer<? super CommonDataSource>>> customizers) {
		return new TraceQueryExecutionListener(beanFactory, dataSourceDecoratorProperties.getIncludes(),
				customizers.getIfAvailable(ArrayList::new));
	}

	@Bean
	@ConditionalOnMissingBean
	ResultSetProxyLogicFactory traceResultSetProxyLogicFactory() {
		return new SimpleResultSetProxyLogicFactory();
	}

}
