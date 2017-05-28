<?php
include 'zbus.php';

$addr = new ServerAddress("localhost:15555", true); 

$msg = new Message();
//$msg->status = 200;
$msg->url = "/tracker";
$msg->topic = "hong";
$msg->token = 'xxxxs';

$msg->set_json_body("jsonxxx"); 
 
$buf = (string)$msg;


$lines = preg_split('/\r\n?/', $buf);

foreach($lines as $line){
	echo $line.'----';
}

echo $buf; 

?> 