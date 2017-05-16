namespace Zbus.Mq
{
    public class Protocol
    {
        public static readonly string VERSION_VALUE = "0.8.0";       //start from 0.8.0 

        //=============================[1] Command Values================================================
        //MQ Produce/Consume
        public static readonly string PRODUCE = "produce";
        public static readonly string CONSUME = "consume";
        public static readonly string ROUTE = "route";     //route back message to sender, designed for RPC 
        public static readonly string RPC = "rpc";       //the same as produce command except rpc set ack false by default

        //Topic control
        public static readonly string DECLARE = "declare";
        public static readonly string QUERY = "query";
        public static readonly string REMOVE = "remove";
        public static readonly string EMPTY = "empty";

        //High Availability (HA) 
        public static readonly string TRACK_PUB = "track_pub";
        public static readonly string TRACK_SUB = "track_sub";
         
        public static readonly string VERSION = "version"; 


        //=============================[2] Parameter Values================================================
        public static readonly string COMMAND = "cmd";
        public static readonly string TOPIC = "topic";
        public static readonly string TOPIC_FLAG = "topic_flag";
        public static readonly string TAG = "tag";
        public static readonly string OFFSET = "offset";

        public static readonly string CONSUME_GROUP = "consume_group";
        public static readonly string CONSUME_GROUP_COPY_FROM = "consume_group_copy_from";
        public static readonly string CONSUME_START_OFFSET = "consume_start_offset";
        public static readonly string CONSUME_START_MSGID = "consume_start_msgid";
        public static readonly string CONSUME_START_TIME = "consume_start_time";
        public static readonly string CONSUME_WINDOW = "consume_window";
        public static readonly string CONSUME_FILTER_TAG = "consume_filter_tag";
        public static readonly string CONSUME_GROUP_FLAG = "consume_group_flag";

        public static readonly string SENDER = "sender";
        public static readonly string RECVER = "recver";
        public static readonly string ID = "id";

        public static readonly string SERVER = "server";
        public static readonly string ACK = "ack";
        public static readonly string ENCODING = "encoding";

        public static readonly string ORIGIN_ID = "origin_id";     //original id, TODO compatible issue: rawid
        public static readonly string ORIGIN_URL = "origin_url";    //original URL  
        public static readonly string ORIGIN_STATUS = "origin_status"; //original Status  TODO compatible issue: reply_code

        //Security 
        public static readonly string TOKEN = "token";


        public static readonly int FLAG_PAUSE = 1 << 0;
        public static readonly int FLAG_RPC = 1 << 1;
        public static readonly int FLAG_EXCLUSIVE = 1 << 2;
        public static readonly int FLAG_DELETE_ON_EXIT = 1 << 3;
    }
}
