package io.zbus.rpc.biz;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;


public interface DbService { 
	User getUser(@Param("userId") String userId);
	 
	Map<String, Object> getProc(@Param("name") String name);
	 
	List<Map<String, Object>> helpCate(); 
}
