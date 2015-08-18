package org.zbus.net;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TicketManager<REQ extends Id, RES extends Id> { 
	
	public static class Ticket<REQ extends Id, RES> {    
		private CountDownLatch latch = new CountDownLatch(1);
		
		private String id = "";
		private REQ request = null; 
		private RES response = null;  
		private ResultCallback<RES> callback = null; 
		 
		private long timeout = 1000; 
		private final long startTime = System.currentTimeMillis(); 
		
		
		public Ticket(REQ request, long timeout) {   
			this.id = uuid(); 
			if(request != null){
				request.setId(this.id);
			}
			
			this.request = request; 
			this.timeout = timeout;
		} 
		
		public static String uuid(){
			return UUID.randomUUID().toString(); 
		}
	 
		public boolean await(long timeout, TimeUnit unit)
				throws InterruptedException {
			boolean status = this.latch.await(timeout, unit); 
			return status;
		}
	 
		public void await() throws InterruptedException {
			this.latch.await(); 
		}
	 
		public void expired() { 
			this.countDown(); 
		}
	 
		private void countDown() {
			this.latch.countDown();
		}
	 
		public boolean isDone() {
			return this.latch.getCount() == 0;
		}
	 
		public void notifyResponse(RES response) {
			this.response = response;
			if (this.callback != null)
				this.callback.onReturn(response); 
			this.countDown();
		} 
	 
		public ResultCallback<RES> getCallback() {
			return callback;
		}
	 
		public void setCallback(ResultCallback<RES> callback) {
			this.callback = callback;
		} 
		 
		public String getId() {
			return id;
		}

		public REQ request() {
			return this.request;
		}
		public RES response() {
			return this.response;
		}
		public long getTimeout() {
			return timeout;
		}
		public long getStartTime() {
			return startTime;
		} 
	}
	
	
	private ConcurrentMap<String, Ticket<REQ, RES>> tickets = new ConcurrentHashMap<String, Ticket<REQ, RES>>();
 
	public Ticket<REQ, RES> getTicket(String id) {
		if(id == null) return null;
		return tickets.get(id);
	}
 
	public Ticket<REQ, RES> createTicket(REQ req, long timeout) {
		return createTicket(req, timeout, null);
	}
 
	
	public Ticket<REQ, RES> createTicket(REQ req, long timeout, ResultCallback<RES> callback) {
		Ticket<REQ, RES> ticket = new Ticket<REQ, RES>(req, timeout);
		ticket.setCallback(callback);

		if (tickets.putIfAbsent(ticket.getId(), ticket) != null) {
			throw new IllegalArgumentException("duplicate ticket number.");
		}

		return ticket;
	} 
	
	public  Ticket<REQ, RES> removeTicket(String id) {
		return tickets.remove(id);
	}
	
}
