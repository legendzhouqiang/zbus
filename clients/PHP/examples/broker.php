<?php  

include '../zbus.php';

$broker = new Broker("localhost:15555");  

$producer = new Producer($broker);

?> 