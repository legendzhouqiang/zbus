package org.zstacks.zbus.proxy.kcxp;

import java.util.ArrayList;
import java.util.List;


public class KCXPResult {
	private String status = "";
	private String errorMessage;
	private List<KCXPTable> tables = new ArrayList<KCXPTable>();

	public List<KCXPTable> getTables(){
		return this.tables;
	}
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void addTable(KCXPTable table){
		this.tables.add(table);
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	
	 
	
	public void dump(){
		if(this.status.equals("0")){
			for(int i=0;i<this.tables.size();i++){
				System.out.println("Table ["+i+"]");
				this.tables.get(i).dump();
			} 
		} else {
			System.out.println("code: "+this.status + ", errorMessage: "+this.errorMessage);
		}
	}

}