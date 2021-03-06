package com.cloudeggtech.granite.cluster.node.mgtnode;

public class Main {
	public static void main(String[] args) throws Exception {
		OptionsTool optionTool = new OptionsTool();
		
		Options options = null;
		try {
			options = optionTool.parseOptions(args);
			
			if (options.isHelp()) {
				optionTool.printUsage();
			}
		} catch (IllegalArgumentException e) {
			if (e.getMessage() != null) {
				System.out.println(String.format("Unable to parse options. %s", e.getMessage()));
			} else {
				System.out.println("Unable to parse options.");
			}
			
			optionTool.printUsage();
			
			return;
		}
		
		options.setDeployDir(options.getHomeDir() + "/deploy");
		options.setAppnodeRuntimesDir(options.getDeployDir() + "/runtimes");
		if (options.getRepositoryDir() == null) {
			options.setRepositoryDir(options.getHomeDir() + "/repository");
		}
		
		// set log directory for logback(see logback.xml)
		System.setProperty("mgtnode.log.dir", options.getHomeDir() + "/log");
		
		try {
			new Starter().start(options);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("There was something wrong. Application terminated.");
		}
		
	}
}
