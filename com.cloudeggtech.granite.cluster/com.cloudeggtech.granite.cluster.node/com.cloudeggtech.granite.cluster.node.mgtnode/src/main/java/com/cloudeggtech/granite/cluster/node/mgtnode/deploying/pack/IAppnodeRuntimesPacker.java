package com.cloudeggtech.granite.cluster.node.mgtnode.deploying.pack;

import com.cloudeggtech.granite.cluster.node.commons.deploying.DeployPlan;

public interface IAppnodeRuntimesPacker {
	void pack(String nodeTypeName, String runtimeName, DeployPlan configuration);
}
