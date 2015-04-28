package org.zstacks.zbus.proxy.tc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TCTable {
	private List<Map<String, byte[]>> data = new ArrayList<Map<String, byte[]>>();

	public int rowSize() {
		return this.data.size();
	}

	public Map<String, byte[]> row(int i) {
		return this.data.get(i);
	}

	public String value(int rowIndex, String key) {
		byte[] val = this.row(rowIndex).get(key);
		return new String(val);
	}
	
	public byte[] getBytes(int rowIndex, String key){
		return this.row(rowIndex).get(key);
	}

	public void addRow(Map<String, byte[]> row) {
		this.data.add(row);
	}

	public void dump() {
		int i = 0;
		for (Map<String, byte[]> kvs : this.data) {
			System.out.println("row[" + i++ + "]:");
			for (Map.Entry<String, byte[]> kv : kvs.entrySet()) {
				System.out.println(kv.getKey() + ": "
							+ new String(kv.getValue())); 
			}
			System.out.println();
		}
	}

}