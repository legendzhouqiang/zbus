package org.zbus.unittest.timer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于时间轮的定时器
 * @ClassName: TimeWheelTimeoutManager  
 * @author 李飞  
 * @date 2016年3月26日 下午11:27:12
 * @since V1.0.0
 */
public class TimerManager {
	
	private static final int DAY_SEC  = 60*60*24;
	private static final int HOUR_SEC = 60*60;
	private static final int MIN_SEC  = 60;
	
	
	/**
	 * 时（0~23）
	 */
	private int nowHour = 0;
	private List<Map<Long, TimerTask>> hourList = new ArrayList<Map<Long, TimerTask>>(24);
	/**
	 * 分(0~59)
	 */
	private int nowMin  = 0;
	private List<Map<Long, TimerTask>> minList = new ArrayList<Map<Long, TimerTask>>(60);
	/**
	 * 秒(0~59)
	 */
	private int nowSec  = 0;
	private List<Map<Long, TimerTask>> secList = new ArrayList<Map<Long, TimerTask>>(60);
	//总秒数
	private int totalSec = 0;
	
	//待启动的定时器队列
	private ConcurrentLinkedQueue<TimerTask> startQueue = new ConcurrentLinkedQueue<TimerTask>();
	//待关闭的定时器ID队列
	private ConcurrentLinkedQueue<Long> stopQueue = new ConcurrentLinkedQueue<Long>();
	//定时器map（主要用于移除定时器用）
	private ConcurrentMap<Long, TimerTask> timerMap = new ConcurrentHashMap<Long, TimerTask>();
	//定时器唯一编号
	private AtomicLong  seq = new AtomicLong(0L);
	
	/**
	 * 每秒滴答调度服务
	 */
	private final ScheduledExecutorService tickScheduler = Executors.newSingleThreadScheduledExecutor();
	
	/**
	 * 执行器服务
	 */
	private final ExecutorService executorService;
	
	public TimerManager(){
		this(getBestPoolSize(),32, 64, 128);
	}
	
	public TimerManager(int nThreads){
		this(nThreads,32, 64, 128);
	}
	
	public TimerManager(int nThreads, int hourMapInitCapacity, int minMapInitCapacity, int secMapInitCapacity){
		Calendar calendar= Calendar.getInstance();
		this.nowHour = calendar.get(Calendar.HOUR_OF_DAY);
		this.nowMin  = calendar.get(Calendar.MINUTE);
		this.nowSec  = calendar.get(Calendar.SECOND);
		this.totalSec = this.nowHour*HOUR_SEC + this.nowMin*MIN_SEC + this.nowSec;
		this.initMapList(this.hourList, 24);
		this.initMapList(this.minList, 60);
		this.initMapList(this.secList, 60);
		//初始化执行线程池
		executorService = Executors.newFixedThreadPool(nThreads);
		//每秒启动一个tick
		tickScheduler.scheduleAtFixedRate(new Runnable(){
			@Override
			public void run() {
				tick();
			}
			
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	private void initMapList(List<Map<Long, TimerTask>> mapList, int count){
		for(int index = 0; index < count; index++){
			Map<Long, TimerTask> map = new HashMap<Long, TimerTask>();
			mapList.add(map);
		}
	}
	
	/**
	 * 停止超时管理器
	 */
	public void shutdown(){
		tickScheduler.shutdown();
		this.executorService.shutdown();
	}
	
	/**
	 * 启动一个定时器
	 *
	 * @param action 超时执行的任务
	 * @param timeout 超时时间（秒），最大值为：一天减1秒
	 * @return 定时器id
	 */
	public long startTimer(Runnable action, int timeout){
		if(timeout > DAY_SEC){
			throw new RuntimeException("max timeout is  " +  (DAY_SEC -1));
		}
		return this.startTimer(action, timeout, false);
	}
	
	/**
	 * 启动一个重复定时器。
	 *
	 * @param action 超时执行的任务
	 * @param period 启动周期（秒），最大值为：一天减1秒
	 * @return 定时器id
	 */
	public long startRepeatingTimer(Runnable action, int period){
		if(period > DAY_SEC){
			throw new RuntimeException("max period is  " +  (DAY_SEC -1));
		}
		return this.startTimer(action, period, true);
	}
	
	private long startTimer(Runnable action, int sec, boolean isRepeat){
		long id = this.seq.incrementAndGet();
		TimerTask timerTask = new TimerTask(id, sec, isRepeat, action); 
		this.startQueue.add(timerTask);
		this.timerMap.put(id, timerTask);
		return id;
	}
	
	/**
	 * 停止一个定时器
	 *
	 * @param timerId 启动定时器时返回的TimerId
	 */
	public void stopTimer(long timerId){
		this.stopQueue.add(timerId);
	}
	
	/**
	 * 秒针每次走一下
	 */
	private void tick(){
		System.out.println(String.format("%2d:%2d:%2d", this.nowHour, this.nowMin, this.nowSec));
		//处理入队，出队
		processStartQueue();
		processStopQueue();
		this.nowSec++;
		this.totalSec++;
		if(this.nowSec > 59){
			//满一分,秒清零
			this.nowSec = 0;
			//分钟变化
			this.nowMin++;
			if( this.nowMin > 59){
				//满一小时,分清零
				this.nowMin = 0;
				//小时变化
				this.nowHour++;
				if(this.nowHour > 23){
					//满一天,时清零
					this.nowHour = 0;
					//总秒数清零
					this.totalSec =0;
				}
				this.onHourChange();
			}
			this.onMinChange();
		}
		this.onSecChange();
	}
	
	/**
	 * 时发生变化
	 */
	private void onHourChange(){
		Map<Long, TimerTask> hourMap = this.hourList.get(this.nowHour);
		for(TimerTask task : hourMap.values()){
			if(task.isTimeout()){
				//超时，直接执行
				processTimeout(task);
			}else{
				//没有超时，放到分级列表
				Map<Long, TimerTask> minMap = this.minList.get(task.getTriggerMin());
				minMap.put(task.getId(), task);
				task.setCurMap(minMap);
			}
		}
		hourMap.clear();
		//this.hourList.set(this.nowHour, null);
	}
	
	/**
	 * 分发生变化
	 */
	private void onMinChange(){
		Map<Long, TimerTask> minMap = this.minList.get(this.nowMin);
		for(TimerTask task : minMap.values()){
			if(task.isTimeout()){
				//超时，直接执行
				processTimeout(task);
			}else{
				//没有超时，放到秒级列表
				Map<Long, TimerTask> secMap = this.secList.get(task.getTriggerSec());
				secMap.put(task.getId(), task);
				task.setCurMap(secMap);
			}
		}
		minMap.clear();
		//this.minList.set(this.nowMin, null);
	}
	
	/**
	 * 秒发生变化
	 */
	private void onSecChange(){
		Map<Long, TimerTask> secMap = this.secList.get(this.nowSec);
		for(TimerTask task : secMap.values()){
			processTimeout(task);
		}
		secMap.clear();
		//this.secList.set(this.nowSec, null);
	}
	
	/**
	 * 执行任务
	 *
	 * @param task
	 */
	private void processTimeout(TimerTask task) {
		this.executorService.execute(task.getAction());
		if(task.isRepeat()){
			this.computeAndSet(task);
		}else{
			this.timerMap.remove(task.getId());
		}
	}
	
	
	/**
	 * 处理开始队列
	 */
	private void processStartQueue(){
		TimerTask timerTask = this.startQueue.poll();
		while(timerTask != null){
			computeAndSet(timerTask);
			timerTask = this.startQueue.poll();
		}
	}
	
	/**
	 * 计算时－分－秒
	 *
	 * @param task
	 */
	protected void computeAndSet(TimerTask task){
		//计算最终触发时分秒
		task.computeTriggerTime();
		//比较时
		int triggerHour = task.getTriggerHour();
		if(this.nowHour != triggerHour){
			Map<Long, TimerTask> map = this.hourList.get(triggerHour);
			map.put(task.getId(), task);
			task.setCurMap(map);
			return;
		}
		//比较分
		int triggerMin = task.getTriggerMin();
		if(this.nowMin != triggerMin){
			Map<Long, TimerTask> map = this.minList.get(triggerMin);
			map.put(task.getId(), task);
			task.setCurMap(map);
			return;
		}
		
		//比较秒
		Map<Long, TimerTask> map = this.secList.get(task.getTriggerSec());
		map.put(task.getId(), task);
		task.setCurMap(map);
	}
	
	/**
	 * 处理停止队列
	 */
	private void processStopQueue(){
		Long timeId = this.stopQueue.poll();
		while(timeId != null){
			TimerTask task = this.timerMap.remove(timeId);
			Map<Long, TimerTask> map = task.getCurMap();
			map.remove(timeId);
			timeId = this.stopQueue.poll();
		}
	}
	
	
	private static int getBestPoolSize() {
        try {
            // JVM可用处理器的个数
            final int cores = Runtime.getRuntime().availableProcessors();
            // 最佳的线程数 = CPU可用核心数 / (1 - 阻塞系数)
            return (int)(cores / (1 - 0.7));
        }
        catch (Exception e) {
            return 16;
        }
    }
    
    private class TimerTask{
    	private final long id;
    	private final int timeout;
    	private final boolean isRepeat; 
    	private int triggerHour = 0;
    	private int triggerMin = 0;
    	private int triggerSec = 0;
    	private final Runnable action;
    	private Map<Long, TimerTask> curMap = null;
		public TimerTask(long id, int timeout, boolean isRepeat, Runnable action) {
			this.timeout = timeout;
			this.action = action;
			this.id = id;
			this.isRepeat = isRepeat;
		}
		
		/**
		 * 判断是否已经超时
		 *
		 * @return
		 */
		public boolean isTimeout(){
			return this.triggerHour == nowHour && this.triggerMin == nowMin && this.triggerSec == nowSec;
		}
		
		/**
		 * 计算触发时间
		 */
		public void computeTriggerTime(){
			//最终的秒数
			int timeoutAt = totalSec + timeout;
			//超过一天的话，去余数
			timeoutAt = timeoutAt % DAY_SEC;
			//计算触发时分秒
			this.triggerHour = timeoutAt/HOUR_SEC;
			timeoutAt = timeoutAt%HOUR_SEC;
			this.triggerMin = timeoutAt/MIN_SEC;
			this.triggerSec = timeoutAt%MIN_SEC;
		}
		
		public final long getId() {
			return id;
		}

		public final Runnable getAction() {
			return action;
		}

		public final int getTriggerHour() {
			return triggerHour;
		}

		public final int getTriggerMin() {
			return triggerMin;
		}

		public final int getTriggerSec() {
			return triggerSec;
		}

		public final Map<Long, TimerTask> getCurMap() {
			return curMap;
		}

		public final void setCurMap(Map<Long, TimerTask> curMap) {
			this.curMap = curMap;
		}

		public final boolean isRepeat() {
			return isRepeat;
		}
    }
    
    private static class TestRun implements Runnable{
    	private final int num;
    	public TestRun(int num){
    		this.num = num;
    	}
		@Override
		public void run() {
			System.out.println(num);
		}
    }
    
    public static void main(String[] args) throws InterruptedException {
    	final TimerManager tm = new TimerManager();
    	//ScheduledThreadPoolExecutor taskScheduler = new ScheduledThreadPoolExecutor(16);
    	Random ra =new Random();
    	for (int i=0;i<10000;i++){
    		int timeout = ra.nextInt(10)+1;
    		tm.startTimer(new TestRun(i), timeout);
    		//taskScheduler.schedule(new TestRun(i), timeout, TimeUnit.SECONDS);
    	}
    }
}
