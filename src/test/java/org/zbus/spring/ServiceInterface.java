package org.zbus.spring;


public interface ServiceInterface { 
	public String echo(String ping);
	public String echo();  
	public int plus(int a, int b);
	public byte[] bin();
}
