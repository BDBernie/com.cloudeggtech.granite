package com.cloudeggtech.granite.xeps.component.processing.internal;

import com.cloudeggtech.granite.framework.core.IService;
import com.cloudeggtech.granite.framework.core.annotations.Component;
import com.cloudeggtech.granite.framework.core.annotations.Dependency;
import com.cloudeggtech.granite.framework.core.integration.IMessageReceiver;

@Component("component.processing.service")
public class ProcessingService implements IService {
	
	@Dependency("parsing.message.receiver")
	private IMessageReceiver parsingMessageReceiver;
	
	
	@Override
	public void start() throws Exception {
		parsingMessageReceiver.start();
	}

	@Override
	public void stop() throws Exception {
		parsingMessageReceiver.stop();
	}

}
