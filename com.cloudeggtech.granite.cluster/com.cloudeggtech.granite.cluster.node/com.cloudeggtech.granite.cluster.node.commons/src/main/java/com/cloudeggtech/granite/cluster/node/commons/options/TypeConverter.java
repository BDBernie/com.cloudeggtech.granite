package com.cloudeggtech.granite.cluster.node.commons.options;

public interface TypeConverter<T> {
	T convert(String name, String value);
}