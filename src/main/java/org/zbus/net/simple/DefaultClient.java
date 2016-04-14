/**
 * The MIT License (MIT)
 * Copyright (c) 2009-2015 HONG LEIMING
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.zbus.net.simple;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.zbus.net.ClientAdaptor;
import org.zbus.net.EventDriver;
import org.zbus.net.Sync.Id;


public class DefaultClient<REQ extends Id, RES extends Id> extends ClientAdaptor<REQ, RES> {   
	private Codec codec;
	private SelectorGroup selectorGroup; 
	
	public DefaultClient(String address, EventDriver driver){
		super(address);
		driver.validateDefault();
		
		selectorGroup = (SelectorGroup)driver.getGroup();
		if(selectorGroup == null){
			throw new IllegalArgumentException("Missing selectorGroup");
		}
		selectorGroup.start(); 
	} 
	 
	@Override
	public void connectAsync() throws IOException {
		if(hasConnected()) return; 
		
		init(); 
		DefaultSession session = (DefaultSession)this.session;
		if (session != null) {
			if (session.isActive() || session.isNew()) {
				return;
			}
		}
		this.session = selectorGroup.registerClientChannel(host, port, this, codec);
	}
	
	private void init(){
		if(codec != null) return;
		if(codecInitializer == null){
			throw new IllegalStateException("Missing codecInitializer");
		}
		List<Object> handlers = new ArrayList<Object>();
		codecInitializer.initPipeline(handlers);
		for(Object handler : handlers){
			if(!(handler instanceof Codec)){
				throw new IllegalArgumentException("Invalid ChannelHandler: " + handler);
			} 
			this.codec = (Codec)handler;
		} 
	}
}
