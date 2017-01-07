package org.zbus.mq.disk;

public class DiskMessage {
	public Long offset; // 8
	public Long timestamp; // 8
	public String id; // 1 + id(max 39)
	public Long corrOffset; // 8
	public String tag; // 1 + tag(max 127)
	public byte[] body; // 4 + len 

	public int size(){
		int bodySize = 0;
		if(body != null) bodySize = body.length;
		return 4 + bodySize + BODY_POS;
	}
	
	public boolean valid = true; //default to valid, when tag not found after reading to the end of block
	public int bytesScanned; //when tagging applied, bytesScanned >= size()
	
	public static final int TAG_POS = 8 + 8 + 40 + 8;
	public static final int BODY_POS = 8 + 8 + 40 + 8 + 128;
}