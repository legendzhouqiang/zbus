<?php
include '../zbus.php';

$addr = new ServerAddress("localhost:15555", true); 

$msg = new Message();
//$msg->status = 200;
$msg->url = "/server";
$msg->topic = "hong";
$msg->token = 'xxxxs';  
$msg->set_json_body("jsonxxx");  
$buf = (string)$msg;


ini_set("log_errors", 1);
ini_set("error_log", "/tmp/php-error.log");

$client = new MessageClient("localhost:15555"); 

$res = $client->invoke($msg); 

echo $res;
?> 