<?php  

$shm_id = shmop_open(0x12345, "c", 0644, 100); 
 
$shm_size = shmop_size($shm_id); 

$shm_bytes_written = shmop_write($shm_id, "my shared memory block", 0); 
 
$my_string = shmop_read($shm_id, 0, $shm_size); 
echo $my_string . "\n";
 

while(true){
	sleep(1);
}
//shmop_delete($shm_id) 
shmop_close($shm_id);


?> 