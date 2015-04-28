package org.zstacks.zbus.proxy.kcxp;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class KCXPTable { 
	private List<Map<String, byte[]>> data; 
	public KCXPTable(){
		this.data = new ArrayList<Map<String,byte[]>>();
	}
	
	public List<Map<String, byte[]>> getData() {
		return data;
	}

	public void setData(List<Map<String, byte[]>> data) {
		this.data = data;
	}

	public void addRow(Map<String, byte[]> row) {
		if (this.data == null) {
			this.data = new ArrayList<Map<String, byte[]>>();
		}
		this.data.add(row);
	}
	
	public void dump(){
		if(this.data != null){
			int i = 0;
			for (Map<String, byte[]> kvs : this.data) {
				System.out.println("row[" + i++ + "]:");
				for (Map.Entry<String, byte[]> kv : kvs.entrySet()) {
					try {
						System.out.println(kv.getKey() + "==> " + new String(kv.getValue(), "GBK"));
					} catch (UnsupportedEncodingException e) { 
						e.printStackTrace();
					}
				}
				System.out.println();
			}
		}
	}

}