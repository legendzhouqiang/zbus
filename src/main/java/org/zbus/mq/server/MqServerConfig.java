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
package org.zbus.mq.server;

import org.zbus.net.EventDriver;

public class MqServerConfig{ 
	public String trackServerList = null;
	
	public String serverHost = "0.0.0.0";
	public int serverPort = 15555;  
	public EventDriver eventDriver;
	public String sslCertificateFile;
	public String sslPrivateKeyFile;
	
	public boolean verbose = false;
	public String storePath = "store";
	public String registerToken = "";  
	public String serverMainIpOrder;
	public boolean mqFilterPersist = false;
	public String serverName = "ZbusServer";
	public long cleanMqInterval = 3000; 
	public long trackReportInterval = 5000;
	
	
	public String getServerAddress(){
		return serverHost + ":" + serverPort;
	}

	public String getTrackServerList() {
		return trackServerList;
	}

	public void setTrackServerList(String trackServerList) {
		this.trackServerList = trackServerList;
	}

	public String getServerHost() {
		return serverHost;
	}

	public void setServerHost(String serverHost) {
		this.serverHost = serverHost;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}
	
	public EventDriver getEventDriver() {
		return eventDriver;
	}

	public void setEventDriver(EventDriver eventDriver) {
		this.eventDriver = eventDriver;
	}

	public boolean isVerbose() {
		return verbose;
	}

	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	public String getStorePath() {
		return storePath;
	}

	public void setStorePath(String storePath) {
		this.storePath = storePath;
	}

	public String getRegisterToken() {
		return registerToken;
	}

	public void setRegisterToken(String registerToken) {
		this.registerToken = registerToken;
	}

	public String getServerMainIpOrder() {
		return serverMainIpOrder;
	}

	public void setServerMainIpOrder(String serverMainIpOrder) {
		this.serverMainIpOrder = serverMainIpOrder;
	}

	public boolean isMqFilterPersist() {
		return mqFilterPersist;
	}

	public void setMqFilterPersist(boolean mqFilterPersist) {
		this.mqFilterPersist = mqFilterPersist;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public long getCleanMqInterval() {
		return cleanMqInterval;
	}

	public void setCleanMqInterval(long cleanMqInterval) {
		this.cleanMqInterval = cleanMqInterval;
	}

	public long getTrackReportInterval() {
		return trackReportInterval;
	}

	public void setTrackReportInterval(long trackReportInterval) {
		this.trackReportInterval = trackReportInterval;
	}

	public String getSslCertificateFile() {
		return sslCertificateFile;
	}

	public void setSslCertificateFile(String sslCertificateFile) {
		this.sslCertificateFile = sslCertificateFile;
	}

	public String getSslPrivateKeyFile() {
		return sslPrivateKeyFile;
	}

	public void setSslPrivateKeyFile(String sslPrivateKeyFile) {
		this.sslPrivateKeyFile = sslPrivateKeyFile;
	} 
	
}