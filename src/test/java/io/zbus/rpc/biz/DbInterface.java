package io.zbus.rpc.biz;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;


public interface DbInterface { 
	@Select("SELECT * FROM  user WHERE User=#{userId}")
	User getUser(@Param("userId") String userId);
	
	@Select("SELECT definer, type, param_list as paramList FROM proc WHERE name = #{name}")
	Map<String, Object> getProc(@Param("name") String name);
	 
	@Select("SELECT * FROM help_category")
	List<Map<String, Object>> helpCate(); 
}
