package org.zbus.pool;

public class PoolConfig {
	
	private int maxTotal = 8;
    private int maxIdle = 8;
    private int minIdle = 0;
    private long minEvictableIdleTimeMillis = 1000L * 60L * 30L;
    private Object support;
	
    public int getMaxTotal() {
		return maxTotal;
	}
	public void setMaxTotal(int maxTotal) {
		this.maxTotal = maxTotal;
	}
	public int getMaxIdle() {
		return maxIdle;
	}
	public void setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
	}
	public int getMinIdle() {
		return minIdle;
	}
	public void setMinIdle(int minIdle) {
		this.minIdle = minIdle;
	}
	public long getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}
	public Object getSupport() {
		return support;
	}
	public void setSupport(Object support) {
		this.support = support;
	}   
}
