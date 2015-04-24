package org.zbus.client.service;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zbus.client.Broker;
import org.zbus.client.broker.SingleBroker;
import org.zbus.client.broker.SingleBrokerConfig;

public class ServiceLoader implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(ServiceLoader.class);
	private Broker broker;
	private boolean ownBroker = false; 

	public ServiceLoader(Broker broker) {
		this.broker = broker;
	}
	
	public ServiceLoader(String brokerAddress){
		SingleBrokerConfig config = new SingleBrokerConfig();
		try {
			this.broker = new SingleBroker(config);
			this.ownBroker = true;
		} catch (IOException e) { 
			log.error(e.getMessage(), e);
		}
	}
	
	public void startService(final ServiceConfig config){
		if (config.getBroker() == null) {
			config.setBroker(broker);
		}
		Service service = new Service(config);
		service.start();
	}
	
	public void loadFromServiceBase(String serviceBase){
		List<File> dirs = getDirs(serviceBase);
		for(File dir : dirs){
			loadFromServiceProject(dir.getAbsolutePath());
		}
	}
	
	public void loadFromServiceProject(final String projectPath){
		//Thread.currentThread().setContextClassLoader 改变
		new Thread(new Runnable() {
			public void run() {
				loadFromServiceProject0(projectPath);
			}
		}).start();
	}
	
	private void loadFromServiceProject0(String projectPath){
		String configFilePath = projectPath + File.separator + "zbus.properties";
		FileInputStream fileInputStream = null;
		Properties props = new Properties();
		try {
			fileInputStream = new FileInputStream(configFilePath);
			props.load(fileInputStream);
			fileInputStream.close();
			
			URL[] urls = getClassLoaderUrls(projectPath);
			ClassLoader classLoader = new ServiceClassLoader(urls); 
			
			List<ServiceConfig> configs = loadServiceConfigs(classLoader, props);
			
			for(final ServiceConfig config : configs){
				startService(config);
			}
			
		} catch (Exception e) { 
			log.error(e.getMessage(), e);
			return;
		}
		
	}
	
	
	
	private URL[] getClassLoaderUrls(String servicePath){
		List<URL> urls = new ArrayList<URL>();
		try { 
			urls.add(new URL("file:" + servicePath + File.separator + "classes" + File.separator));
			List<File> jars = getJars(servicePath + File.separator + "lib") ;
			for(File jar : jars){
				urls.add(new URL("file:" + jar.getAbsolutePath()));
			}
		} catch (MalformedURLException e) { 
			//ignore
		}
		return urls.toArray(new URL[0]);
	}
 
	private static List<File> getDirs(String basePath) {
		List<File> dirs = new ArrayList<File>();
		try {
			File base = new File(basePath);
			if (base.isDirectory() && base.exists()) {
				for (File dir : base.listFiles()) {
					if (dir.isDirectory() && dir.exists()) {
						dirs.add(dir);
					}
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		return dirs;
	}
	
	private static List<File> getJars(String basePath) {
		List<File> jars = new ArrayList<File>();
		try {
			File base = new File(basePath);
			if (base.isDirectory() && base.exists()) {
				for (File file : base.listFiles()) {
					if (file.isFile() && file.exists()) { 
						jars.add(file);
					} 
					if (file.isDirectory() && file.exists()) { 
						jars.addAll(getDirs(file.getAbsolutePath()));
					} 
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}
		return jars;
	}


	private static List<ServiceConfig> loadServiceConfigs(final ClassLoader classLoader, Properties props){
		List<ServiceConfig> configs = new ArrayList<ServiceConfig>();
		final String namespace = "zbus.mq";
		final String prefix = namespace+".";
		String mqNameString = props.getProperty(namespace);
		if(mqNameString == null) return configs;
		String[] mqs = mqNameString.split(",");
		for(String mq : mqs){
			mq = mq.trim();
			if(mq.equals("")) continue; 
			
			String provider = props.getProperty(prefix + mq + ".provider", "").trim();
			if("".equals(provider)){
				log.warn("missing provider for MQ="+mq);
				continue;
			}
			String consumerCount = props.getProperty(prefix + mq + ".consumerCount", "1").trim();
			String accessToken = props.getProperty(prefix + mq + ".accessToken", "").trim();
			String registerToken = props.getProperty(prefix + mq + ".registerToken", "").trim();
			
			
			int threadCount = 1;
			try{
				threadCount = Integer.valueOf(consumerCount);
			} catch (Exception e){
				log.warn(e.getMessage(), e);
				//ignore
			}
			ServiceConfig config = new ServiceConfig();
			config.setMq(mq);
			config.setAccessToken(accessToken.trim());
			config.setRegisterToken(registerToken.trim());
			config.setThreadCount(threadCount);
			 
			Class<?> providerClass = null;
			try { 
				providerClass = classLoader.loadClass(provider);
			} catch (ClassNotFoundException e) { 
				log.warn(e.getMessage(), e);
				continue;
			}
			//
			if(!ServiceProvider.class.isAssignableFrom(providerClass)){
				log.warn(provider+" is not a type of "+ServiceProvider.class);
				continue;
			}
			try {
				final ServiceProvider sp = (ServiceProvider)providerClass.newInstance();
				config.setServiceHandler(sp.buildHandler());
				configs.add(config);
			} catch (Exception e) {
				log.warn(e.getMessage(), e);
			} 
		}
		
		return configs;
	}

	@Override
	public void close() throws IOException {
		if(this.ownBroker && this.broker != null){
			this.broker.close();
		}
	}
	
	public static void main(String[] args) throws Exception{
		ServiceLoader loader = new ServiceLoader("127.0.0.1:15555");
		loader.close();
	}
}