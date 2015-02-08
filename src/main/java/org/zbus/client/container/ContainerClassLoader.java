package org.zbus.client.container;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.zbus.client.Broker;
import org.zbus.client.container.Scanner.Listener;
import org.zbus.client.container.Scanner.ScanInfo;
import org.zbus.server.ZbusServer;

public class ContainerClassLoader extends ClassLoader {
	private ChildClassLoader childClassLoader;

	public ContainerClassLoader(URL[] urls) {
		super(Thread.currentThread().getContextClassLoader());
		childClassLoader = new ChildClassLoader(urls, new DetectClass(
				this.getParent()));
	}

	@Override
	protected synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		try {
			if(name.startsWith("org.zbus")){ //ZBUS shared
				return super.loadClass(name, resolve);
			}
			return childClassLoader.findClass(name);
		} catch (ClassNotFoundException e) {
			return super.loadClass(name, resolve);
		}
	}

	private class ChildClassLoader extends URLClassLoader {
		private DetectClass realParent;

		public ChildClassLoader(URL[] urls, DetectClass realParent) {
			super(urls, null);
			this.realParent = realParent;
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			try {
				Class<?> loaded = super.findLoadedClass(name);
				if (loaded != null)
					return loaded;
				return super.findClass(name);
			} catch (ClassNotFoundException e) {
				return realParent.loadClass(name);
			}
		}
	}

	private class DetectClass extends ClassLoader {
		public DetectClass(ClassLoader parent) {
			super(parent);
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
			return super.findClass(name);
		}
	}

	
	public static void main(String[] args) throws Exception {
		Scanner scanner = new ClassScanner();
		scanner.addJarpath("G:/TradingCoder/tradespace/java-lab/ext_lib"); 
		
		final List<URL> urls = new ArrayList<URL>();
		scanner.scanJar(new Listener() {

			@Override
			public void onScanned(ScanInfo info) {
				System.out.println(info);
				try {
					URL url = new URL("file:" + info.jarpath);
					urls.add(url);
				} catch (MalformedURLException e) {
				}

			}
		});
		 
		final ContainerClassLoader loader = new ContainerClassLoader(urls.toArray(new URL[0]));
		scanner.scanClass(new Listener() {
			
			@Override
			public void onScanned(ScanInfo info) {  
				if("org.zbus.client.broker.HaBroker".equals(info.className)){
					System.out.println(info);
					try {
						Class<?> clazz = loader.loadClass(info.className);
						
						for(Class<?> c : clazz.getInterfaces()){
							if(c == Broker.class){
								System.out.println("!!!!!!!!!!");
							}
							System.out.println(c);
						}
						
					} catch (ClassNotFoundException e) { 
						e.printStackTrace();
					}
				}
			}
		});

		Class<?> zbus = loader.loadClass("org.zbus.server.ZbusServer");
		//loader = new MyClassLoader(urls.toArray(new URL[0]));
		Class<?> zbus2 = loader.loadClass("org.zbus.server.ZbusServer"); 
		
		Class<?> zbus3 = ZbusServer.class; 
		System.out.println(zbus == zbus2);
		System.out.println(zbus == zbus3); 
	}

}
