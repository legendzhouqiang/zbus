
##Message Queue Design

	Index
		--> Block1  
		--> Block2
		--> Block3

	
	MappedMemoryFile
	
		 INDEX
	+---------------+
	| Version      4|
	+---------------+
	| BlockCount   4|
	+---------------+
	| BlockStart   8|  -------+
	+---------------+         |
	| Flag         4|         |
	+---------------+         |
	| MessageCount 8|         | 
	+---------------+         |% MaxBlockSize (Circle)
	|       .       |         |
	|       .       |         |
	|  0~1023 Header|         |
	+---------------+         |
	| BlockOffset_0 |  <------+
	+---------------+
	| BlockOffset_1 |
	+---------------+
	| BlockOffset_n |
	+---------------+
	
	
	BlockOffset Format:
	+------------------------------------------------------------------------+
	| [8: baseOffset] | [8: createdTime] | [4: endOffset] | [8: updatedTime] |
	+------------------------------------------------------------------------+


	BlockFile: {baseOffset}.zbus, e.g. 00000000000000123456.zbus
	
	DiskMessage Format
	+---------------+
	| Offset       8|
	+---------------+
	| Timestamp    8|
	+---------------+
	| Id          40|   
	+---------------+    
	| CorrOffset   8|        
	+---------------+         
	| MessageCount 8|          
	+---------------+         
	| Tag        128|  
	+---------------+
	| Len          4|
	+---------------+
	| Body         ?|
	+---------------+ 

