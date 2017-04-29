package io.zbus.net;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.zbus.kit.FileKit;
import io.zbus.kit.logging.Logger;
import io.zbus.kit.logging.LoggerFactory;

public class EventDriver implements Closeable {
	private static final Logger log = LoggerFactory.getLogger(EventDriver.class);

	private EventLoopGroup bossGroup;  
	private EventLoopGroup workerGroup;  
	private final boolean ownBossGroup;
	private final boolean ownWorkerGroup; 
	
	private SslContext sslContext; 
	private int idleTimeInSeconds = 300; //5 minutes
	private int packageSizeLimit = 1024*1024*32; //maximum of 32M

	public EventDriver() {
		try {
			bossGroup = new NioEventLoopGroup();
			workerGroup = new NioEventLoopGroup();
			ownBossGroup = ownWorkerGroup = true;
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public EventDriver(EventLoopGroup group){
		this.bossGroup = group;
		this.workerGroup = group;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}

	public EventDriver(EventLoopGroup bossGroup, EventLoopGroup workerGroup) {
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
		this.ownBossGroup = false;
		this.ownWorkerGroup = false;
	}
	
	public EventDriver(EventDriver driver){
		this(driver.bossGroup, driver.workerGroup);
		this.idleTimeInSeconds = driver.idleTimeInSeconds;
		this.packageSizeLimit = driver.packageSizeLimit;
		this.sslContext = driver.sslContext;
	}

	public EventDriver duplicate(){ 
		return new EventDriver(this); 
	}
	
	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}


	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	} 
	
	public EventLoopGroup getGroup() {
		// try bossGroup first
		if (bossGroup != null)
			return bossGroup;
		//then workerGroup
		return workerGroup;
	}

	public SslContext getSslContext() {
		return sslContext;
	}
	
	public boolean isSslEnabled() {
		return sslContext != null;
	} 

	public void setSslContext(SslContext sslContext) { 
		this.sslContext = sslContext;
	} 
	
	public void setServerSslContext(InputStream certStream, InputStream privateKeyStream) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forServer(certStream, privateKeyStream);
			this.sslContext = builder.build(); 
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public void setServerSslContext(String certStreamPath, String privateKeyPath) { 
		InputStream certStream = FileKit.inputStream(certStreamPath);
		if(certStream == null){
			throw new IllegalArgumentException("Certification file(" + certStreamPath + ") not exists");
		}
		InputStream privateKeyStream = FileKit.inputStream(privateKeyPath);
		if(privateKeyStream == null){
			try {
				certStream.close();
			} catch (IOException e) {
				//ignore
			}
			throw new IllegalArgumentException("PrivateKey file(" + privateKeyPath + ") not exists"); 
		}
		
		setServerSslContext(certStream, privateKeyStream);
		
		try {
			certStream.close();
		} catch (IOException e) {
			//ignore
		} 
		try {
			privateKeyStream.close();
		} catch (IOException e) {
			//ignore
		} 
	}
	
	public void setClientSslContext(InputStream certStream) { 
		try {
			SslContextBuilder builder = SslContextBuilder.forClient().trustManager(certStream);
			this.sslContext = builder.build();
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}
	
	public void setClientSslContext(String certStreamPath) { 
		InputStream certStream = FileKit.inputStream(certStreamPath);
		if(certStream == null){
			throw new IllegalArgumentException("Certification file(" +certStreamPath + ") not exists");
		}
		
		setClientSslContext(certStream);
		try {
			certStream.close();
		} catch (IOException e) {
			//ignore
		}
	}

	public void setServerSslContextOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate());
			InputStream privateKeyStream = new FileInputStream(cert.privateKey());
			setServerSslContext(certStream, privateKeyStream);
			try{
				certStream.close();
			} catch(IOException e) {
				//ignore
			}
			try{
				privateKeyStream.close();
			} catch(IOException e) {
				//ignore
			}
			
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}
	
	public void setClientSslContextOfSelfSigned() { 
		try {
			SelfSignedCertificate cert = new SelfSignedCertificate(); 
			InputStream certStream = new FileInputStream(cert.certificate()); 
			setClientSslContext(certStream);
			
			try{
				certStream.close();
			} catch(IOException e) {
				//ignore
			}
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	@Override
	public void close() throws IOException {
		if (ownBossGroup && bossGroup != null) {
			bossGroup.shutdownGracefully(); 
			bossGroup = null;
		}
		if (ownWorkerGroup && workerGroup != null) {
			workerGroup.shutdownGracefully();
			workerGroup = null;
		}
	}

	public int getIdleTimeInSeconds() {
		return idleTimeInSeconds;
	}

	public void setIdleTimeInSeconds(int idleTimeInSeconds) {
		this.idleTimeInSeconds = idleTimeInSeconds;
	}

	public int getPackageSizeLimit() {
		return packageSizeLimit;
	}

	public void setPackageSizeLimit(int packageSizeLimit) {
		this.packageSizeLimit = packageSizeLimit;
	}   
	
}
