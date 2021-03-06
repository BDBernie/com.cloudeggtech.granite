package com.cloudeggtech.granite.framework.core.internal.repository;


import org.osgi.framework.BundleContext;

import com.cloudeggtech.granite.framework.core.IService;
import com.cloudeggtech.granite.framework.core.repository.AbstractComponentInfo;
import com.cloudeggtech.granite.framework.core.repository.CreationException;
import com.cloudeggtech.granite.framework.core.repository.IComponentInfo;

public final class GenericComponentInfo extends AbstractComponentInfo implements IComponentInfo {
	
	public GenericComponentInfo(String id, Class<?> type, BundleContext bundleContext) {
		super(id, type, bundleContext, true);
	}
	
	public boolean isService() {
		return IService.class.isAssignableFrom(type);
	}
	
	public String toString() {
		if (isService()) {
			return String.format("Service[%s, %s]", id, type);
		} else {
			return String.format("Component[%s, %s]", id, type);
		}
	}

	@Override
	public Object doCreate() throws CreationException {
		try {
			return getType().newInstance();
		} catch (Exception e) {
			throw new CreationException("Can't create component", e);
		}
	}

	@Override
	public IComponentInfo getAliasComponent(String alias) {
		return new GenericComponentInfo(alias, type, bundleContext);
	}
}
