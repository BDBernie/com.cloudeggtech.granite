package com.cloudeggtech.granite.cluster.node.mgtnode.deploying.pack;

import com.cloudeggtech.granite.cluster.node.commons.deploying.DeployPlan;

public interface IPackModule {
	String[] getDependedModules();
	CopyBundleOperation[] getCopyBundles();
	IPackConfigurator getCallback();
	
	void copyBundles(IPackContext context);
	void configure(IPackContext context, DeployPlan configuration);
	
	boolean isBundlesCopied();
	boolean isConfigured();
}
