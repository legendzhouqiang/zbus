<?php
include '../zbus.php'; 
 
$client = new MqClient("localhost:15555"); 

$msg = new Message();
$msg->url = "/server";  

$res = $client->invoke($msg);  
$client->close();


echo $res;
?> 