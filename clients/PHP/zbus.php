<?php 
class Protocol {  
	const VERSION_VALUE = "0.8.0";       //start from 0.8.0 
	
	//=============================[1] Command Values================================================
	//MQ Produce/Consume
	const PRODUCE       = "produce";   
	const CONSUME       = "consume";  
	const ROUTE   	    = "route";     //route back message to sender, designed for RPC 
	const RPC   	    = "rpc";       //the same as produce command except rpc set ack false by default
	
	//Topic control
	const DECLARE_ = "declare";   //declare and empty keywords!!! PHP sucks
	const QUERY   = "query"; 
	const REMOVE  = "remove";
	const EMPTY_   = "empty";   
	
	//High Availability (HA) 
	const TRACK_PUB   = "track_pub"; 
	const TRACK_SUB   = "track_sub";  
	const TRACKER     = "tracker";    
	
	//=============================[2] Parameter Values================================================
	const COMMAND       	   = "cmd";     
	const TOPIC         	   = "topic";
	const TOPIC_MASK           = "topic_mask"; 
	const TAG   	     	   = "tag";  
	const OFFSET        	   = "offset";
	
	const CONSUME_GROUP        = "consume_group";  
	const GROUP_START_COPY     = "group_start_copy";  
	const GROUP_START_OFFSET   = "group_start_offset";
	const GROUP_START_MSGID    = "group_start_msgid";
	const GROUP_START_TIME     = "group_start_time";   
	const GROUP_FILTER         = "group_filter";  
	const GROUP_MASK           = "group_mask"; 
	const CONSUME_WINDOW       = "consume_window";  
	
	const SENDER   			= "sender"; 
	const RECVER   			= "recver";
	const ID      		    = "id";	   
	
	const HOST   		    = "host";  
	const ACK      			= "ack";	  
	const ENCODING 			= "encoding"; 
	
	const ORIGIN_ID         = "origin_id";
	const ORIGIN_URL   		= "origin_url";
	const ORIGIN_STATUS     = "origin_status";
	
	//Security 
	const TOKEN   		    = "token"; 
	
	
	const MASK_PAUSE    	  = 1<<0; 
	const MASK_RPC    	      = 1<<1; 
	const MASK_EXCLUSIVE 	  = 1<<2;  
	const MASK_DELETE_ON_EXIT = 1<<3; 
}

function log_info($message){
	error_log($message); 
}
function log_debug($message){
	error_log($message); 
}
function log_error($message){
	error_log($message); 
}

//borrowed from: https://stackoverflow.com/questions/2040240/php-function-to-generate-v4-uuid
function uuid() {
	return sprintf( '%04x%04x-%04x-%04x-%04x-%04x%04x%04x',
		// 32 bits for "time_low"
		mt_rand( 0, 0xffff ), mt_rand( 0, 0xffff ),
		
		// 16 bits for "time_mid"
		mt_rand( 0, 0xffff ),
		
		// 16 bits for "time_hi_and_version",
		// four most significant bits holds version number 4
		mt_rand( 0, 0x0fff ) | 0x4000,
		
		// 16 bits, 8 bits for "clk_seq_hi_res",
		// 8 bits for "clk_seq_low",
		// two most significant bits holds zero and one for variant DCE1.1
		mt_rand( 0, 0x3fff ) | 0x8000,
		
		// 48 bits for "node"
		mt_rand( 0, 0xffff ), mt_rand( 0, 0xffff ), mt_rand( 0, 0xffff )
	);
}

class ServerAddress {
	public $address;
	public $sslEnabled;

	function __construct($address, $sslEnabled = false) {  
		if(is_string($address)){
			$this->address = $address;
			$this->sslEnabled = $sslEnabled; 
			return;
		}
		
		$type = get_class($address);
		if($type == ServerAddress::class){
			$this->address = $address->address;
			$this->sslEnabled = $address->sslEnabled;
		}
		
	}
	
	public function __toString(){
		if($this->sslEnabled){
			return "[SSL]".$this->address;
		}
		return $this->address;
	}  
} 

class Message {
	public $status;          //integer
	public $method = "GET";
	public $url = "/";
	public $headers = array();
	public $body;  
	 
	
	public function set_json_body($value){
		$this->headers['content-type'] = 'application/json';
		$this->body = $value; 
	}
	
	public function __set($name, $value){
		if($value == null) return;
		$this->headers[$name] = $value;
	}
	
	public function __get($name){
		if(!array_key_exists($name, $this->headers)) return null;
		return $this->headers[$name];
	}
	
	public function __toString(){
		return $this->encode();
	}
	
	
	public function encode(){
		$res = "";
		$desc = "unknown status";
		if($this->status){
			if(array_key_exists($this->status, Message::HTTP_STATUS_TABLE)){
				$desc = Message::HTTP_STATUS_TABLE[$this->status];
			}
			$res .= sprintf("HTTP/1.1 %s %s\r\n",$this->status, $desc);
		} else {
			$res .= sprintf("%s %s HTTP/1.1\r\n",$this->method?:"GET", $this->url?:"/");
		}
		foreach($this->headers as $key=>$value){
			if($key == 'content-length'){
				continue;
			}
			$res .= sprintf("%s: %s\r\n", $key, $value);
		}
		$body_len = 0;
		if($this->body){
			$body_len = strlen($this->body);
		}
		$res .= sprintf("content-length: %d\r\n", $body_len);
		$res .= sprintf("\r\n");
		if($this->body){
			$res .= $this->body;
		}
		
		return $res;
	}
	
	public static function decode($buf, $start=0){
		$p = strpos($buf, "\r\n\r\n", $start);
		if($p < 0) return array(null, $start);
		
		$head = substr($buf, $start, $p);
		$msg = Message::decode_headers($head);
		$body_len = 0;
		if(array_key_exists('content-length', $msg->headers)){
			$body_len = $msg->headers['content-length'];
			$body_len = intval($body_len);
		} 
		if( $body_len == 0) return array($msg, $p); 
		
		if(strlen($buf)-$p < $body_len){
			return array(null, $start);
		}
		$msg->body = substr($buf, $p+4, $body_len); 
		return array($msg, $p + $body_len); 
	}
	 
	private static function decode_headers($buf){
		$msg = new Message();
		$lines = preg_split('/\r\n?/', $buf); 
		$meta = $lines[0];
		$blocks = explode(' ', $meta); 
		if(substr(strtoupper($meta), 0, 4 ) == "HTTP"){
			$msg->status = intval($blocks[0]);
		} else {
			$msg->method = strtoupper($blocks[0]);
			if(count($blocks) > 1){
				$msg->url = $blocks[1];
			}
		} 
		for($i=1; $i<count($lines); $i++){
			$line = $lines[$i]; 
			$kv = explode(':', $line); 
			if(count($kv) < 2) continue;
			$key = strtolower(trim($kv[0]));
			$val = trim($kv[1]); 
			$msg->headers[$key] = $val;
		} 
		return $msg;
	}  
	
	static $HTTP_STATUS_TABLE;
}

Message::$HTTP_STATUS_TABLE = array(
	200 => "OK",
	201 => "Created",
	202 => "Accepted",
	204 => "No Content",
	206 => "Partial Content",
	301 => "Moved Permanently",
	304 => "Not Modified",
	400 => "Bad Request",
	401 => "Unauthorized",
	403 => "Forbidden",
	404 => "Not Found",
	405 => "Method Not Allowed",
	416 => "Requested Range Not Satisfiable",
	500 => "Internal Server Error",
);


class MessageClient{
	private $server_address; 
	private $ssl_cert_file;
	private $sock;
	private $recv_buf;
	private $result_table = array();
	
	function __construct($address, $ssl_cert_file=null){
		$this->server_address = new ServerAddress($address);
		$this->ssl_cert_file = $ssl_cert_file;
	}
	
	public function connect() {
		$this->sock = socket_create(AF_INET, SOCK_STREAM, 0);
		$address = $this->server_address->address;
		$bb = explode(':', $address);
		$host = $bb[0];
		$port = 80;
		if(count($bb) > 1){
			$port = intval($bb[1]);
		}
		
		log_debug("Trying connect to ($this->server_address)");
		if (!socket_connect($this->sock, $host, $port)){ 
			$this->throw_socket_exception("Connection to ($address) failed");
		} 
		log_debug("Connected to ($this->server_address)"); 
	}  
	
	public function close(){
		if($this->sock){
			socket_close($this->sock);
			$this->sock = null;
		}
	}
	
	private function throw_socket_exception($msg_prefix=null){
		$errorcode = socket_last_error($this->sock);
		$errormsg = socket_strerror($errorcode);
		$msg = "${msg_prefix}, $errorcode:$errormsg";
		log_error($msg);
		throw new Exception($msg);
	}
	
	
	public function invoke($msg, $timeout=3){
		$msgid = $this->send($msg, $timeout);
		return $this->recv($msgid, $timeout);
	}
	
	public function send($msg, $timeout=3){
		if($this->sock == null){
			$this->connect();
		}
		if($msg->id == null){
			$msg->id = uuid();
		}
		$buf = $msg->encode();
		log_debug($buf);
		$sending_buf = $buf;
		$write_count = 0;
		$total_count = strlen($buf);
		while(true){
			$n = socket_write($this->sock, $sending_buf, strlen($sending_buf));
			if($n === false) {
				$this->throw_socket_exception("Socket write error");
			}
			$write_count += $n;
			if($write_count>=$total_count) return;
			if($n > 0){
				$sending_buf = substr($sending_buf, $n);
			}
		}
		return $msg->id;
	}
	
	public function recv($msgid=null, $timeout=3){ 
		if($this->sock == null){
			$this->connect();
		}
		
		$all_buf = '';
		while(true) {
			if($msgid && array_key_exists($msgid, $this->result_table)){
				return $this->result_table[$msgid];
			}
			
			$buf_len = 4096;
			$buf = socket_read($this->sock, $buf_len);  
			if($buf === false){
				$this->throw_socket_exception("Socket read error");
			}
			$all_buf .= $buf;
			
			$this->recv_buf .= $buf;
			$start = 0;
			while(true) {
				$res = Message::decode($this->recv_buf, $start);
				$msg = $res[0];
				$start = $res[1];
				if($msg == null) {
					if($start!= 0) {
						$this->recv_buf = substr($this->recv_buf, $start); 
					}
					break;
				}
				$this->recv_buf = substr($this->recv_buf, $start); 
				
				if($msgid != null){
					if($msgid != $msg->id){
						$this->result_table[$msg->id] = $msg;
						continue;
					}
				}
				log_debug($all_buf);
				return $msg;
			}   
		} 
	}
}


class MqClient extends MessageClient{
	public $token;
	
	public function __construct($address, $ssl_cert_file=null){
		parent::__construct($address, $ssl_cert_file);
	}
	
	private function invoke_cmd($cmd, $topic_or_msg, $group=null, $timeout=3){
		
	}
	
	public function query($topic, $group=null, $timeout=3){
		$msg = new Message();
		$msg->topic = $topic;
		$msg->topic = $group;
		$msg->token = $this->token;
		
		
		
	}
}


?> 