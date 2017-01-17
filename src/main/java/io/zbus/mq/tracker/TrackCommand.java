package io.zbus.mq.tracker;

public interface TrackCommand {  
	public static final String EntryUpdate   = "entry_update"; 
	public static final String EntryRemove   = "entry_remove";
	public static final String ServerJoin    = "server_join";
	public static final String ServerLeave   = "server_leave";
	
	public static final String PubAll        = "pub_all"; 
	public static final String SubAll        = "sub_all";   
}
