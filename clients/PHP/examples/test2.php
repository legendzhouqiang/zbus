<?php  
 
$shm_id = shmop_open(0x12345, "c", 0644, 100); 
  
$my_string = shmop_read($shm_id, 0, shmop_size($shm_id)); 
echo $my_string . "\n";
  

?> 