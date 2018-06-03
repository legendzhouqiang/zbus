package io.zbus.mq;

import static io.zbus.kit.ConfigKit.valueOf;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import io.zbus.auth.ApiKeyProvider;
import io.zbus.auth.XmlApiKeyProvider;
import io.zbus.kit.ConfigKit.XmlConfig;

public class MqServerConfig extends XmlConfig {
	public String host;
	public Integer port;
	public boolean sslEnabled = false;
	public String sslCertFile;
	public String sslKeyFile;
	public int maxSocketCount = 102400;
	public int packageSizeLimit = 1024 * 1024 * 64; // 64M
	public String mqDir = "/tmp/zbus";
	public ApiKeyProvider apiKeyProvider;

	public MqServerConfig() {

	}

	public MqServerConfig(String configXmlFile) {
		loadFromXml(configXmlFile);
	}

	@Override
	public void loadFromXml(Document doc) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		this.host = valueOf(xpath.evaluate("/zbus/host", doc), "0.0.0.0");
		this.port = valueOf(xpath.evaluate("/zbus/port", doc), 15555);

		this.sslEnabled = valueOf(xpath.evaluate("/zbus/sslEnabled", doc), false);
		this.sslCertFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@certFile", doc), null);
		this.sslKeyFile = valueOf(xpath.evaluate("/zbus/sslEnabled/@keyFile", doc), null);

		this.maxSocketCount = valueOf(xpath.evaluate("/zbus/maxSocketCount", doc), 102400);
		String size = valueOf(xpath.evaluate("/zbus/packageSizeLimit", doc), "64M");
		size = size.toUpperCase();
		if (size.endsWith("M")) {
			this.packageSizeLimit = Integer.valueOf(size.substring(0, size.length() - 1)) * 1024 * 1024;
		} else if (size.endsWith("G")) {
			this.packageSizeLimit = Integer.valueOf(size.substring(0, size.length() - 1)) * 1024 * 1024 * 1024;
		} else {
			this.packageSizeLimit = Integer.valueOf(size);
		}

		if (valueOf(xpath.evaluate("/zbus/auth", doc), null) != null) {
			XmlApiKeyProvider provider = new XmlApiKeyProvider();
			provider.loadFromXml(doc);
			apiKeyProvider = provider;
		}
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public int getMaxSocketCount() {
		return maxSocketCount;
	}

	public void setMaxSocketCount(int maxSocketCount) {
		this.maxSocketCount = maxSocketCount;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}

	public String getMqDir() {
		return mqDir;
	}

	public void setMqDir(String mqDir) {
		this.mqDir = mqDir;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public String getSslCertFile() {
		return sslCertFile;
	}

	public void setSslCertFile(String sslCertFile) {
		this.sslCertFile = sslCertFile;
	}

	public String getSslKeyFile() {
		return sslKeyFile;
	}

	public void setSslKeyFile(String sslKeyFile) {
		this.sslKeyFile = sslKeyFile;
	}

	public ApiKeyProvider getApiKeyProvider() {
		return apiKeyProvider;
	}

	public void setApiKeyProvider(ApiKeyProvider apiKeyProvider) {
		this.apiKeyProvider = apiKeyProvider;
	} 
}
