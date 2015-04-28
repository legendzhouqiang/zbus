package org.zstacks.zbus.proxy.tc;

import java.util.ArrayList;
import java.util.List;

public class TCResult {
	private List<TCTable> tables = new ArrayList<TCTable>();

	public List<TCTable> getTables() {
		return this.tables;
	}

	public void addTable(TCTable table) {
		this.tables.add(table);
	}

	public void dump() {
		for (int i = 0; i < this.tables.size(); i++) {
			System.out.println("Table [" + i + "]");
			this.tables.get(i).dump();
		}
	} 
}