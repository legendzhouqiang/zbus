<?php
include('../zbus.php');

$sock = socket_create(AF_INET, SOCK_STREAM, 0);

socket_connect($sock, 'localhost', 15555);
$msg = "GET /tracker HTTP/1.1 \r\n\r\n";

socket_send($sock, $msg, strlen($msg), 0);
socket_recv($sock, $buf, 10240, MSG_WAITALL);

echo $buf; 
 
?>