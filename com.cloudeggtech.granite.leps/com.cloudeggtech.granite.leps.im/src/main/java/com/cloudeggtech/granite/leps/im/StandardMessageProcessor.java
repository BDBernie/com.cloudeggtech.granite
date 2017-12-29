package com.cloudeggtech.granite.leps.im;

import com.cloudeggtech.basalt.protocol.im.stanza.Message;
import com.cloudeggtech.granite.framework.core.annotations.Dependency;
import com.cloudeggtech.granite.framework.core.event.IEventService;
import com.cloudeggtech.granite.framework.core.event.IEventServiceAware;
import com.cloudeggtech.granite.framework.processing.IMessageProcessor;
import com.cloudeggtech.granite.framework.processing.IProcessingContext;

public class StandardMessageProcessor implements IMessageProcessor, IEventServiceAware {
	@Dependency("chat.message.deliverer")
	private IChatMessageDeliverer deliverer;
	
	private IEventService eventService;

	@Override
	public boolean process(IProcessingContext context, Message message) {
		if (!deliverer.isMessageDeliverable(context, message))
			return false;
		
		deliverer.deliver(context, eventService, message);
		
		return true;
	}

	@Override
	public void setEventService(IEventService eventService) {
		this.eventService = eventService;
	}
}
