function serverTopicList(topicMap){
	var res = "";
	var keys = Object.keys(topicMap);
	keys.sort();
	
	for(var i in keys){ 
		res += "<a href='#' class='topic link label label-info'>" + keys[i] + "</a>";
	} 
   	return res;
}

function buildTopicList(serverInfo){
	var topicMap = serverInfo.topicMap
	var res = "";
	var keys = Object.keys(topicMap);
	keys.sort();
	
	for(var i in keys){ 
		res += "<span address='" + serverInfo.serverAddress + "' class='topic-list topic label label-success'>" + keys[i] + "</span>";
	} 
   	return res;
}  


function showServerTable(trackBroker){ 
	$("#server-list").find("tr:gt(0)").remove();
	
	var serverInfoMap = trackBroker.serverInfoMap;
	for(var key in serverInfoMap){
		var serverInfo = serverInfoMap[key];
		var topicList = serverTopicList(serverInfo.topicMap); 
		var checked ="checked=checked";
		var filterServerList = trackBroker.filterServerList;
		if(filterServerList && !filterServerList.includes(key)){
			checked = "";
		}
		var tag = "";
		if(serverInfo.serverAddress == trackBroker.serverAddress){
			tag = "<span>*</span>";
		}
		$("#server-list").append(
			"<tr>\
				<td><a class='link' target='_blank' href='http://"+serverInfo.serverAddress + "'>" + serverInfo.serverAddress + "</a>\
					"+ tag + "<div class='filter-box'>\
	            		<input class='server' type='checkbox' "+ checked +" value='"+serverInfo.serverAddress + "'>\
	            	</div>\
            	</td>\
				<td>" + serverInfo.serverVersion + "</td>\
				<td>\
	                <span class='badge'>" + hashSize(serverInfo.topicMap) + "</span>" + topicList + "\
	           	</td>\
			</tr>"
		);    
	} 
} 



function showServerList(trackBroker){  
	$("#server-list").find("tr:gt(0)").remove();
	
	var serverInfoMap = trackBroker.serverInfoMap;
	for(var key in serverInfoMap){
		var serverInfo = serverInfoMap[key];
		var topicList = buildTopicList(serverInfo); 
		var checked ="checked=checked";
		var filterServerList = trackBroker.filterServerList;
		if(filterServerList && !filterServerList.includes(key)){
			checked = "";
		}
		
		$("#server-list").append(
			"<tr>\
				<td>\
					<a class='link' target='_blank' href='http://"+serverInfo.serverAddress + "'>" + serverInfo.serverAddress + "</a>\
					<div class='filter-box'>\
	            		<input class='server' type='checkbox' "+ checked +" value='"+serverInfo.serverAddress + "'>\
	            	</div>\
            	</td>\
				<td>" + serverInfo.serverVersion + "</td>\
				<td>\
	                <span class='topic topic-badge label label-success label-as-badge'>" + hashSize(serverInfo.topicMap) + "</span>" + topicList + "\
	           	</td>\
			</tr>"
		);    
	} 
}
 

function consumeGroupList(groupList){ 
	var res = "";
	for(var i in groupList){ 
		var group = groupList[i];
		res += "<tr>";
		res += "<td>" + group.groupName + "</td>";
		res += "<td>" + group.messageCount + "</td>";
		res += "<td>" + group.consumerCount + "</td>";
		var filterTag = "";
		if(group.filterTag){
			filterTag = group.filterTag;
		}
		res += "<td>" + filterTag + "</td>";
		res += "</tr>"
	} 
	return res;
}

function topicServerList(serverList){
	var res = ""; 
	serverList.sort(); 
	for(var i in serverList){ 
		var topicInfo = serverList[i];
		var linkAddr = topicInfo.serverAddress;
		res += "<tr>";
		//link td
		res += "<td><a class='topic' target='_blank' href='http://" + linkAddr + "'>" + linkAddr + "</a></td>";
		
		//message depth td
		res += "<td>" + topicInfo.messageDepth + "</td>"; 
		
		//consume group td
		res += "<td> <table class='table-nested cgroup'> " + consumeGroupList(topicInfo.consumeGroupList) + "</table></td>";
		
		res += "</tr>"; 
	} 
   	return res;
}

function showTopicTable(trackBroker){ 
	$("#topic-list").find("tr:gt(2)").remove();
	var topicSumMap = trackBroker.topicSumMap;
	var topics = [];
	for(var key in topicSumMap){
		topics.push(key);
	}
	topics.sort();
	for(var i in topics){
		var topicName = topics[i];
		var topicSum = topicSumMap[topicName];
		var serverList = topicServerList(topicSum); 
		$("#topic-list").append(
			"<tr id="+topicName+">\
				<td><a class='topic' data-toggle='modal' data-target='#topic-modal'>" +topicName + "</a></td>\
				<td><table class='table-nested sgroup'>"+ serverList + "</table></td>\
			</tr>"
   		); 
	}  
} 




function showModalServerList(trackBroker, topic){  
	 
}

function showModalConsumeGroupList(trackBroker, topic){  
	 
}
 