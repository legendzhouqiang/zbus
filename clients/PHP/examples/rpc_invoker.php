<?php 
include '../zbus.php';

//start with a broker
$broker = new Broker("localhost:15555");

$rpc = new RpcInvoker($broker, "MyRpc");

$req = new Request("plus", array(1, 2));

$res = $rpc->invoke($req);

echo $res . "\n";


$res = $rpc->plus(1,2);

echo $res . "\n";

//close broker
$broker->close();

?> 