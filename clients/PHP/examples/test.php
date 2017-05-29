<?php 
include '../zbus.php';
$client = new MqClient("localhost:15555");

$res = $client->query("MyTopic");
  
echo $res->topicName;


?> 