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

package io.micrometer.tracing.brave.instrument.messaging;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.XAConnection;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;

import brave.jms.JmsTracing;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.endpoint.JmsMessageEndpointManager;

/**
 * {@link BeanPostProcessor} wrapping around JMS {@link ConnectionFactory}.
 *
 * @author Adrian Cole
 * @since 2.1.0
 */
public class TracingConnectionFactoryBeanPostProcessor implements BeanPostProcessor {

	private final BeanFactory beanFactory;

	public TracingConnectionFactoryBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		// Wrap the caching connection factories instead of its target, because it catches
		// callbacks
		// such as ExceptionListener. If we don't wrap, cached callbacks like this won't
		// be traced.
		if (bean instanceof CachingConnectionFactory factory) {
			return new LazyConnectionFactory(this.beanFactory, factory);
		}
		if (bean instanceof JmsMessageEndpointManager manager) {
			MessageListener listener = manager.getMessageListener();
			if (listener != null) {
				manager.setMessageListener(new LazyMessageListener(this.beanFactory, listener));
			}
			return bean;
		}
		if (bean instanceof XAConnectionFactory factory && bean instanceof ConnectionFactory factory) {
			return new LazyConnectionAndXaConnectionFactory(this.beanFactory, factory,
					factory);
		}
		// We check XA first in case the ConnectionFactory also implements
		// XAConnectionFactory
		else if (bean instanceof XAConnectionFactory factory) {
			return new LazyXAConnectionFactory(this.beanFactory, factory);
		}
		else if (bean instanceof TopicConnectionFactory factory) {
			return new LazyTopicConnectionFactory(this.beanFactory, factory);
		}
		else if (bean instanceof ConnectionFactory factory) {
			return new LazyConnectionFactory(this.beanFactory, factory);
		}
		return bean;
	}

}

class LazyXAConnectionFactory implements XAConnectionFactory {

	private final BeanFactory beanFactory;

	private final XAConnectionFactory delegate;

	private JmsTracing jmsTracing;

	private XAConnectionFactory wrappedDelegate;

	LazyXAConnectionFactory(BeanFactory beanFactory, XAConnectionFactory delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public XAConnection createXAConnection() throws JMSException {
		return wrappedDelegate().createXAConnection();
	}

	@Override
	public XAConnection createXAConnection(String s, String s1) throws JMSException {
		return wrappedDelegate().createXAConnection(s, s1);
	}

	@Override
	public XAJMSContext createXAContext() {
		return wrappedDelegate().createXAContext();
	}

	@Override
	public XAJMSContext createXAContext(String s, String s1) {
		return wrappedDelegate().createXAContext(s, s1);
	}

	private JmsTracing jmsTracing() {
		if (this.jmsTracing != null) {
			return this.jmsTracing;
		}
		this.jmsTracing = this.beanFactory.getBean(JmsTracing.class);
		return this.jmsTracing;
	}

	private XAConnectionFactory wrappedDelegate() {
		if (this.wrappedDelegate != null) {
			return this.wrappedDelegate;
		}
		this.wrappedDelegate = jmsTracing().xaConnectionFactory(this.delegate);
		return this.wrappedDelegate;
	}

}

class LazyTopicConnectionFactory implements TopicConnectionFactory {

	private final BeanFactory beanFactory;

	private final TopicConnectionFactory delegate;

	private final LazyConnectionFactory factory;

	private JmsTracing jmsTracing;

	LazyTopicConnectionFactory(BeanFactory beanFactory, TopicConnectionFactory delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
		this.factory = new LazyConnectionFactory(beanFactory, delegate);
	}

	@Override
	public TopicConnection createTopicConnection() throws JMSException {
		return jmsTracing().topicConnection(this.delegate.createTopicConnection());
	}

	@Override
	public TopicConnection createTopicConnection(String s, String s1) throws JMSException {
		return jmsTracing().topicConnection(this.delegate.createTopicConnection(s, s1));
	}

	@Override
	public Connection createConnection() throws JMSException {
		return this.factory.createConnection();
	}

	@Override
	public Connection createConnection(String s, String s1) throws JMSException {
		return this.factory.createConnection(s, s1);
	}

	@Override
	public JMSContext createContext() {
		return this.factory.createContext();
	}

	@Override
	public JMSContext createContext(String s, String s1) {
		return this.factory.createContext(s, s1);
	}

	@Override
	public JMSContext createContext(String s, String s1, int i) {
		return this.factory.createContext(s, s1, i);
	}

	@Override
	public JMSContext createContext(int i) {
		return this.factory.createContext(i);
	}

	private JmsTracing jmsTracing() {
		if (this.jmsTracing != null) {
			return this.jmsTracing;
		}
		this.jmsTracing = this.beanFactory.getBean(JmsTracing.class);
		return this.jmsTracing;
	}

}

class LazyConnectionFactory implements ConnectionFactory {

	private final BeanFactory beanFactory;

	private final ConnectionFactory delegate;

	private JmsTracing jmsTracing;

	private ConnectionFactory wrappedDelegate;

	LazyConnectionFactory(BeanFactory beanFactory, ConnectionFactory delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public Connection createConnection() throws JMSException {
		return wrappedDelegate().createConnection();
	}

	@Override
	public Connection createConnection(String s, String s1) throws JMSException {
		return wrappedDelegate().createConnection(s, s1);
	}

	@Override
	public JMSContext createContext() {
		return wrappedDelegate().createContext();
	}

	@Override
	public JMSContext createContext(String s, String s1) {
		return wrappedDelegate().createContext(s, s1);
	}

	@Override
	public JMSContext createContext(String s, String s1, int i) {
		return wrappedDelegate().createContext(s, s1, i);
	}

	@Override
	public JMSContext createContext(int i) {
		return wrappedDelegate().createContext(i);
	}

	private JmsTracing jmsTracing() {
		if (this.jmsTracing != null) {
			return this.jmsTracing;
		}
		this.jmsTracing = this.beanFactory.getBean(JmsTracing.class);
		return this.jmsTracing;
	}

	private ConnectionFactory wrappedDelegate() {
		if (this.wrappedDelegate != null) {
			return this.wrappedDelegate;
		}
		this.wrappedDelegate = jmsTracing().connectionFactory(this.delegate);
		return this.wrappedDelegate;
	}

}

class LazyConnectionAndXaConnectionFactory implements ConnectionFactory, XAConnectionFactory {

	private final ConnectionFactory connectionFactoryDelegate;

	private final XAConnectionFactory xaConnectionFactoryDelegate;

	LazyConnectionAndXaConnectionFactory(BeanFactory beanFactory, ConnectionFactory connectionFactoryDelegate,
			XAConnectionFactory xaConnectionFactoryDelegate) {
		this.connectionFactoryDelegate = new LazyConnectionFactory(beanFactory, connectionFactoryDelegate);
		this.xaConnectionFactoryDelegate = new LazyXAConnectionFactory(beanFactory, xaConnectionFactoryDelegate);
	}

	@Override
	public Connection createConnection() throws JMSException {
		return this.connectionFactoryDelegate.createConnection();
	}

	@Override
	public Connection createConnection(String userName, String password) throws JMSException {
		return this.connectionFactoryDelegate.createConnection(userName, password);
	}

	@Override
	public JMSContext createContext() {
		return this.connectionFactoryDelegate.createContext();
	}

	@Override
	public JMSContext createContext(String userName, String password) {
		return this.connectionFactoryDelegate.createContext(userName, password);
	}

	@Override
	public JMSContext createContext(String userName, String password, int sessionMode) {
		return this.connectionFactoryDelegate.createContext(userName, password, sessionMode);
	}

	@Override
	public JMSContext createContext(int sessionMode) {
		return this.connectionFactoryDelegate.createContext(sessionMode);
	}

	@Override
	public XAConnection createXAConnection() throws JMSException {
		return this.xaConnectionFactoryDelegate.createXAConnection();
	}

	@Override
	public XAConnection createXAConnection(String userName, String password) throws JMSException {
		return this.xaConnectionFactoryDelegate.createXAConnection(userName, password);
	}

	@Override
	public XAJMSContext createXAContext() {
		return this.xaConnectionFactoryDelegate.createXAContext();
	}

	@Override
	public XAJMSContext createXAContext(String userName, String password) {
		return this.xaConnectionFactoryDelegate.createXAContext(userName, password);
	}

}

class LazyMessageListener implements MessageListener {

	private final BeanFactory beanFactory;

	private final MessageListener delegate;

	private JmsTracing jmsTracing;

	LazyMessageListener(BeanFactory beanFactory, MessageListener delegate) {
		this.beanFactory = beanFactory;
		this.delegate = delegate;
	}

	@Override
	public void onMessage(Message message) {
		wrappedDelegate().onMessage(message);
	}

	private JmsTracing jmsTracing() {
		if (this.jmsTracing != null) {
			return this.jmsTracing;
		}
		this.jmsTracing = this.beanFactory.getBean(JmsTracing.class);
		return this.jmsTracing;
	}

	private MessageListener wrappedDelegate() {
		// Adds a consumer span as we have no visibility into JCA's implementation of
		// messaging
		return jmsTracing().messageListener(this.delegate, true);
	}

}
