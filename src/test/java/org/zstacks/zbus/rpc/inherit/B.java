package org.zstacks.zbus.rpc.inherit;


public abstract class B implements A{
	public abstract String echo(String value);
	public String getString(){
		return "B";
	}
}
