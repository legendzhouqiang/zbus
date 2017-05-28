<?php
include '../zbus.php';

$addr = new ServerAddress("localhost:15555", true); 

$msg = new Message();
//$msg->status = 200;
$msg->url = "/tracker";
$msg->topic = "hong";
$msg->token = 'xxxxs';
 
$msg->set_json_body("jsonxxx");  
$buf = (string)$msg;

echo $buf . PHP_EOL;

$msg2 = Message::decode($buf)[0];

echo $msg2;
?> 