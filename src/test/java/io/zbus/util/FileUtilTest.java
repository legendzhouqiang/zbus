package io.zbus.util;

import java.util.HashMap;
import java.util.Map;

public class FileUtilTest {

	public static void main(String[] args) throws Exception { 
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("sub", 1.02);
		String res = FileUtil.loadTemplate("template/test.htm", model);
		System.out.println(res);
	}

}
