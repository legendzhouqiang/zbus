<?php
include '../zbus.php'; 
 
$client = new MqClient("localhost:15555"); 

$msg = new Message();
$msg->cmd = "consume";  
$msg->topic = "hong";

$res = $client->invoke($msg);  
$client->close();


echo $res;
?> 