#include "Protocol.h"  
const string Protocol::VERSION_VALUE = "0.8.0";  //start from 0.8.0 

//=============================[1] Command Values================================================
//MQ Produce/Consume
const string Protocol::PRODUCE = "produce";
const string Protocol::CONSUME = "consume";
const string Protocol::ROUTE = "route";     //route back message to sender, designed for RPC 
const string Protocol::RPC = "rpc";       //the same as produce command except rpc set ack false by default

//Topic control
const string Protocol::DECLARE = "declare";
const string Protocol::QUERY = "query";
const string Protocol::REMOVE = "remove";
const string Protocol::EMPTY = "empty";

//High Availability (HA) 
const string Protocol::TRACK_PUB = "track_pub";
const string Protocol::TRACK_SUB = "track_sub";
const string Protocol::TRACKER = "tracker";
const string Protocol::SERVER = "server";
 

//=============================[2] Parameter Values================================================
const string Protocol::CMD = "cmd";
const string Protocol::TOPIC = "topic";
const string Protocol::TOPIC_MASK = "topic_mask";
const string Protocol::TAG = "tag";
const string Protocol::OFFSET = "offset";

const string Protocol::CONSUME_GROUP = "consume_group";
const string Protocol::GROUP_START_COPY = "group_start_copy";
const string Protocol::GROUP_START_OFFSET = "group_start_offset";
const string Protocol::GROUP_START_MSGID = "group_start_msgid";
const string Protocol::GROUP_START_TIME = "group_start_time";
const string Protocol::GROUP_FILTER = "group_filter";
const string Protocol::GROUP_MASK = "group_mask";
const string Protocol::CONSUME_WINDOW = "consume_window";

const string Protocol::SENDER = "sender";
const string Protocol::RECVER = "recver";
const string Protocol::ID = "id";

const string Protocol::HOST = "host";
const string Protocol::ACK = "ack";
const string Protocol::ENCODING = "encoding";

const string Protocol::ORIGIN_ID = "origin_id";
const string Protocol::ORIGIN_URL = "origin_url";
const string Protocol::ORIGIN_STATUS = "origin_status";

//Security 
const string Protocol::TOKEN = "token"; 

const int Protocol::MASK_PAUSE = 1 << 0;
const int Protocol::MASK_RPC = 1 << 1;
const int Protocol::MASK_EXCLUSIVE = 1 << 2;
const int Protocol::MASK_DELETE_ON_EXIT = 1 << 3; 