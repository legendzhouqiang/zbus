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
package org.zbus.ha;

import java.io.Closeable;
import java.util.List;

import org.zbus.broker.Broker;
import org.zbus.broker.Broker.BrokerHint;
import org.zbus.net.http.Message;
import org.zbus.net.http.MessageClient;

/**
 * Broker selector interface for HA broker, default implementation is DefaultBrokerSelector
 * 
 * @author HONG LEIMING 
 *
 */
public interface BrokerSelector extends Closeable{
	/**
	 * Select single broker based the hint, which includes Server,EntryID,SourceIP etc
	 * @param hint
	 * @return best broker matched for the hint
	 */
	Broker selectByBrokerHint(BrokerHint hint);
	/**
	 * Select best broker(s) based on the request message content
	 * Criteria could be Server/EntryId(MQ) etc.
	 * @param msg
	 * @return List of brokers for PubSub, list of single broker otherwise
	 */
	List<Broker> selectByRequestMsg(Message msg);
	/**
	 * Provide a mechanism to resolve the entry abstraction in a Message
	 * @param msg
	 * @return
	 */
	String getEntry(Message msg);
	/**
	 * Select a broker based on the MessageClient.
	 * This is usually implemented by tagging a client when being generated.
	 * 
	 * @param client
	 * @return
	 */
	Broker selectByClient(MessageClient client);
}