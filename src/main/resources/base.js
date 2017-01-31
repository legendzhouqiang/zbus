Date.prototype.Format = function (fmt) { //author: meizz 
    var o = {
        "M+": this.getMonth() + 1, 
        "d+": this.getDate(), 
        "h+": this.getHours(), 
        "m+": this.getMinutes(), 
        "s+": this.getSeconds(),  
        "q+": Math.floor((this.getMonth() + 3) / 3),  
        "S": this.getMilliseconds()  
    };
    if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    for (var k in o)
    if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
    return fmt;
}

function timeConverter(unixTime){ 
	var d = new Date(unixTime);
	return d.Format("yyyy-MM-dd hh:mm:ss"); 
	//return d.getFullYear()+"/"+(d.getMonth()+1)+"/"+d.getDate()+" "+d.getHours()+":"+d.getMinutes();
}

function dictSize(dict){
	return Object.keys(dict).length
}

function toTopicSummary(serverTable){
	var topicSumMap = {};
	for(var key in serverTable){
		if(key == '@type') continue;
		var serverInfo = serverTable[key];
		var topicMap = serverInfo.topicMap;
		for(var topic in topicMap){
			if(topic == '@type') continue;
			var topicSum = {'messageDepth': 0, 'consumerGroupCount': 0, 'consumerCount': 0};
			var topicInfo = topicMap[topic];
			if(topic in topicSumMap){
				topicSum = topicSumMap[topic]; 
			} else {
				topicSumMap[topic] = topicSum;
				
			}
			topicSum.messageDepth += topicInfo.messageCount; 
			topicSum.consumerGroupCount += topicInfo.consumerGroupCount; 
			topicSum.consumerCount += topicInfo.consumerCount
		}
	}
	return topicSumMap;
}