package com.cloudeggtech.granite.cluster.integration;

import com.cloudeggtech.granite.framework.core.annotations.Component;
import com.cloudeggtech.granite.framework.core.connection.IConnectionContext;
import com.cloudeggtech.granite.framework.core.integration.IMessage;
import com.cloudeggtech.granite.framework.core.integration.IMessageChannel;
import com.cloudeggtech.granite.framework.core.session.ISession;

@Component("cluster.any.2.routing.message.receiver")
public class Any2RoutingMessageReceiver extends LocalMessageIntegrator {
	private static final String CONFIGURATION_KEY_ANY_2_ROUTING_MESSAGE_QUEUE_MAX_SIZE = "any.2.routing.message.queue.max.size";
	private static final int DEFAULT_MESSAGE_QUEUE_MAX_SIZE = 1024 * 64;
	
	@Override
	protected int getDefaultMessageQueueMaxSize() {
		return DEFAULT_MESSAGE_QUEUE_MAX_SIZE;
	}
	
	@Override
	protected String getMessageQueueMaxSizeConfigurationKey() {
		return CONFIGURATION_KEY_ANY_2_ROUTING_MESSAGE_QUEUE_MAX_SIZE;
	}

	@Override
	protected IConnectionContext doGetConnectionContext(ISession session) {
		return new RoutingConnectionContext(messageChannel, session);
	}
	
	private class RoutingConnectionContext extends AbstractConnectionContext {

		public RoutingConnectionContext(IMessageChannel messageChannel, ISession session) {
			super(messageChannel, session);
		}
		
		@Override
		public void close() {
			throw new UnsupportedOperationException("Can't call close operation in routing phase.");
		}
		
		@Override
		protected boolean isMessageAccepted(Object message) {
			Class<?> messageType = message.getClass();
			if (!IMessage.class.isAssignableFrom(messageType))
				return false;
			
			Object payload = ((IMessage)message).getPayload();
			
			return String.class == payload.getClass();
		}
	}

	@Override
	protected String getOsgiServicePid() {
		return Constants.ANY_2_ROUTING_MESSAGE_INTEGRATOR;
	}
}
