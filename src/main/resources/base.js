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


function topicServerList(serverList){
	var res = ""; 
	serverList.sort();
	var len = serverList.length;
	
	for(var i in serverList){ 
		var link = "<div><a class='topic' target='_blank' href='http://" + serverList[i] + "'>" + serverList[i] + "</a>";
		if(len>1){
			link += "<span class='label label-info'>"+(parseInt(i)+1)+"</span>";
		} 
		link += "<div>";
		res += link
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


function showTopicTable(trackBroker){ 
	$("#topic-list").find("tr:gt(0)").remove();
	var topicSumMap = trackBroker.topicSumMap;
	var topics = [];
	for(var key in topicSumMap){
		topics.push(key);
	}
	topics.sort();
	for(var i in topics){
		var topicName = topics[i];
		var topicSum = topicSumMap[topicName];
		var serverList = topicServerList(topicSum.serverList);
		var filterTagList = "";
		$("#topic-list").append(
			"<tr id="+topicName+">\
				<td><a class='topic' data-toggle='modal' data-target='#topic-modal'>" +topicName + "</a></td>\
				<td>"+ serverList + "</td>\
				<td>"+ topicSum.messageDepth + "</td>\
				<td>"+ topicSum.messageActive + "</td>\
				<td>"+ filterTagList + "</td>\
				<td>"+ topicSum.consumerIdle + " / " + topicSum.consumerTotal + "</td>\
				<td>"+ topicSum.consumerGroupCount+ "</td>\
			</tr>"
   		); 
	}  
} 




function showModalServerList(trackBroker, topic){  
	$("#modal-server-list").find("tr:gt(0)").remove();
	
	var serverInfoMap = trackBroker.serverInfoMap;
	for(var key in serverInfoMap){
		var serverInfo = serverInfoMap[key];
		var topicMap = serverInfo.topicMap; 
		var topicInfo = topicMap[topic];
		if(!topicInfo) continue; 
		
		var checked ="checked=checked";
		var filterServerList = trackBroker.modalFilterServerList;
		if(filterServerList && !filterServerList.includes(key)){
			checked = "";
		}
		
		var msgActive = 0;
		var msgFilter = "";//TODO
		var idleCount = 0;
		for(var key in topicInfo.consumerGroupList){
			var cg = topicInfo.consumerGroupList[key];
			msgActive += cg.messageCount; 
			idleCount += cg.consumerCount;
		}
		
		$("#modal-server-list").append(
			"<tr>\
				<td>\
					<a class='link' target='_blank' href='http://"+serverInfo.serverAddress + "'>" + serverInfo.serverAddress + "</a>\
					<div class='filter-box'>\
	            		<input class='server' type='checkbox' "+ checked +" value='"+serverInfo.serverAddress + "'>\
	            	</div>\
            	</td>\
				<td>" + topicInfo.messageCount + "</td>\
				<td>" + msgActive + "</td>\
				<td>" + msgFilter + "</td>\
				<td> (" + idleCount + " / " + idleCount + ") </td>\
				<td>" + topicInfo.consumerGroupCount + "</td>\
			</tr>"
		);    
	} 
}

function showModalConsumerGroupList(trackBroker, topic){  
	$("#modal-group-list").find("tr:gt(0)").remove();
	
	var serverInfoMap = trackBroker.serverInfoMap;
	for(var key in serverInfoMap){
		var serverInfo = serverInfoMap[key];
		var topicMap = serverInfo.topicMap; 
		var topicInfo = topicMap[topic];
		if(!topicInfo) continue; 
		
		var checked ="checked=checked";
		var filterServerList = trackBroker.modalFilterServerList;
		if(filterServerList && !filterServerList.includes(key)){
			checked = "";
		}
		
		var msgActive = 0;
		var msgFilter = "";//TODO
		var idleCount = 0;
		for(var key in topicInfo.consumerGroupList){
			var cg = topicInfo.consumerGroupList[key];  
			$("#modal-group-list").append(
				"<tr>\
					<td>\
						<a class='link' href='#'>" + cg.groupName + "</a>\
						<div class='filter-box'>\
		            		<input class='server' type='checkbox' "+ checked +" value=''>\
		            	</div>\
	            	</td>\
					<td>" + cg.messageCount + "</td>\
					<td>" + msgFilter + "</td>\
					<td> (" + cg.consumerCount + " / " + cg.consumerCount + ") </td>\
					<td>" + serverInfo.serverAddress + "</td>\
				</tr>"
			);    
		}
	} 
}
 