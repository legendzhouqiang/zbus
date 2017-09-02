package io.zbus.mq.server.auth;

import java.util.HashMap;
import java.util.Map;

import io.zbus.mq.Protocol;

public enum Operation { 
	ADMIN(0),
	
	PRODUCE(1),
	RPC(1),       //same order as produce
	
	CONSUME(2),
	UNCONSUME(2), //same order as consume
	
	ROUTE(3),
	QUERY(4),
	DECLARE(5), 
	EMPTY(6),
	REMOVE(7),
	TRACK_PUB(8),
	TRACK_SUB(9),
	TRACKER(10),
	SERVER(11);    
	
	private final int mask; 
	private Operation(int bit){ 
        this.mask = 1<<bit;
    } 
	public final int getMask() {
        return mask;
    } 
	
	private static final Map<String, Operation> table = new HashMap<String, Operation>(); 
	static {
		table.put("admin", Operation.ADMIN);
		table.put(Protocol.PRODUCE, Operation.PRODUCE);
		table.put(Protocol.RPC, Operation.RPC);
		table.put(Protocol.CONSUME, Operation.CONSUME);
		table.put(Protocol.UNCONSUME, Operation.UNCONSUME);
		table.put(Protocol.ROUTE, Operation.ROUTE);
		table.put(Protocol.QUERY, Operation.QUERY);
		table.put(Protocol.DECLARE, Operation.DECLARE);
		table.put(Protocol.EMPTY, Operation.EMPTY);
		table.put(Protocol.REMOVE, Operation.REMOVE);
		table.put(Protocol.TRACK_PUB, Operation.TRACK_PUB);
		table.put(Protocol.TRACK_SUB, Operation.TRACK_SUB);
		table.put(Protocol.TRACKER, Operation.TRACKER);
		table.put(Protocol.SERVER, Operation.SERVER);
	}  
    
	public static Operation fromCommand(String command){ //use instead of valueOf
		return table.get(command);
	}
	
    public static boolean isEnabled(int mask, Operation operation) {
        return (mask & operation.getMask()) != 0;
    } 
    
    public static boolean isEnabled(int mask, String op) {
    	Operation operation = table.get(op.toLowerCase()); //case in-sensitive
    	if(operation == null) return false;
    	return (mask & operation.getMask()) != 0; 
    } 
    
    public static int intValue(Operation... operations){
    	int value = 0;
    	for(Operation op : operations){
    		value |= op.mask;
    	}
    	return value;
    }
}
