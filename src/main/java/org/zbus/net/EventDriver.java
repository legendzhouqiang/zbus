package org.zbus.net;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import org.zbus.kit.ClassKit;
import org.zbus.kit.log.Logger;

public class EventDriver implements Closeable {
	private static final Logger log = Logger.getLogger(EventDriver.class);

	private Object bossGroup;  
	private Object workerGroup;  
	private Object sslContext;

	private boolean ownBossGroup = true;
	private boolean ownWorkerGroup = true; 

	public EventDriver() {
		try {
			bossGroup = ClassKit.nettyNioEventLoopGroupClass.newInstance();
			workerGroup = ClassKit.nettyNioEventLoopGroupClass.newInstance();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public EventDriver(Object bossGroup, Object workerGroup) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}

	public Object getBossGroup() {
		return bossGroup;
	}

	public void setBossGroup(Object bossGroup) {
		if (this.bossGroup != null && ownBossGroup) {
			closeGroup(this.bossGroup);
		}
		this.bossGroup = bossGroup;
	}

	public Object getWorkerGroup() {
		return workerGroup;
	}

	public void setWorkerGroup(Object workerGroup) {
		if (this.workerGroup != null && ownWorkerGroup) {
			closeGroup(this.workerGroup);
		}
		this.workerGroup = workerGroup;
	}

	public Object getGroup() {// try bossGroup first, then workerGroup, if only
								// one set
		if (bossGroup != null)
			return bossGroup;
		return workerGroup;
	}

	public Object getSslContext() {
		return sslContext;
	}

	public void setSslContext(Object sslContext) {
		log.info("SSL: Enabled");
		this.sslContext = sslContext;
	}

	public void setSslContext(File certFile, File privateKeyFile) {
		if (!ClassKit.nettyAvailable) {
			throw new IllegalStateException("SSL requires Netty support");
		}
		try {
			Method method = ClassKit.nettySslContextBuilderClass.getMethod("forServer", File.class, File.class);
			Object builder = method.invoke(null, certFile, privateKeyFile);
			this.setSslContext(ClassKit.invokeSimple(builder, "build"));
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	public void setSslContextOfSelfSigned() {
		if (!ClassKit.nettyAvailable) {
			throw new IllegalStateException("SSL requires Netty support");
		}

		try {
			Object certObj = ClassKit.nettySelfSignedCertificateClass.newInstance();
			File certFile = (File) ClassKit.invokeSimple(certObj, "certificate");
			File privateKeyFile = (File) ClassKit.invokeSimple(certObj, "privateKey");
			setSslContext(certFile, privateKeyFile);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void close() throws IOException {
		if (ownBossGroup) {
			closeGroup(bossGroup);
		}
		if (ownWorkerGroup) {
			closeGroup(ownWorkerGroup);
		}
	}

	private static void closeGroup(Object group) {
		if (group == null)
			return;
		Class<?> nettyClass = ClassKit.nettyEventLoopGroupClass;
		if (nettyClass != null && nettyClass.isAssignableFrom(group.getClass())) {
			try {
				ClassKit.invokeSimple(group, "shutdownGracefully");
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return;
		}
		if (group instanceof Closeable) {
			Closeable r = (Closeable) group;
			try {
				r.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	} 
	public boolean isSslEnabled() {
		return sslContext != null;
	}

	private static void checkNettyGroup(String name, Object group) {
		if (group != null) {
			Class<?> groupClass = ClassKit.nettyEventLoopGroupClass;
			if (groupClass == null) {
				throw new IllegalStateException("Netty unavailable");
			}
			if (!groupClass.isAssignableFrom(group.getClass())) {
				throw new IllegalStateException(name + " should instance of " + groupClass.getName());
			}
		}
	}

	public void validateNetty() {
		checkNettyGroup("bossGroup", bossGroup);
		checkNettyGroup("workerGroup", workerGroup);
	}  
}
