using System;
using System.Collections.Generic;

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
        public static readonly string TOPIC_MASK = "topic_mask";
        public static readonly string TAG = "tag";
        public static readonly string OFFSET = "offset";

        public static readonly string CONSUME_GROUP = "consume_group";
        public static readonly string GROUP_START_COPY = "group_start_copy";
        public static readonly string GROUP_START_OFFSET = "group_start_offset";
        public static readonly string GROUP_START_MSGID = "group_start_msgid";
        public static readonly string GROUP_START_TIME = "group_start_time"; 
        public static readonly string GROUP_FILTER = "group_filter";
        public static readonly string GROUP_MASK = "group_mask";
        public static readonly string CONSUME_WINDOW = "consume_window";

        public static readonly string SENDER = "sender";
        public static readonly string RECVER = "recver";
        public static readonly string ID = "id";

        public static readonly string SERVER = "server";
        public static readonly string ACK = "ack";
        public static readonly string ENCODING = "encoding";

        public static readonly string ORIGIN_ID = "origin_id";      
        public static readonly string ORIGIN_URL = "origin_url";     
        public static readonly string ORIGIN_STATUS = "origin_status";  

        //Security 
        public static readonly string TOKEN = "token";


        public static readonly int MASK_PAUSE = 1 << 0;
        public static readonly int MASK_RPC = 1 << 1;
        public static readonly int MASK_EXCLUSIVE = 1 << 2;
        public static readonly int MASK_DELETE_ON_EXIT = 1 << 3;
    } 

    public class ServerAddress
    {
        public string Address { get; set; }
        public bool SslEnabled { get; set; }

        public ServerAddress()
        {

        }
        public ServerAddress(string address)
        {
            this.Address = address;
            this.SslEnabled = false;
        }

        public ServerAddress(IDictionary<string, object> dict) //from java/js object
        {
            if (dict.ContainsKey("address"))
            {
                this.Address = (string)dict["address"];
            }
            if (dict.ContainsKey("sslEnabled"))
            {
                this.SslEnabled = (bool)dict["sslEnabled"];
            }
        }
    }

    public class ErrorInfo
    {
        public string Error { get; set; } //used only for batch operation indication
    }

    public class TrackItem : ErrorInfo
    {
        public ServerAddress ServerAddress { get; set; }
        public string ServerVersion { get; set; }
        
    }

    public class TrackerInfo : TrackItem
    {
        public List<ServerAddress> TrackedServerList { get; set; }
    }

    public class ServerInfo : TrackerInfo
    {
        public List<ServerAddress> TrackerList { get; set; }
        public IDictionary<string, TopicInfo> TopicTable { get; set; }
    }

    public class TopicInfo : TrackItem
    {
        public string TopicName { get; set; }
        public int Mask { get; set; }

        public long MessageDepth { get; set; } //message count on disk
        public int ConsumerCount { get; set; } //total consumer count in consumeGroupList
        public List<ConsumeGroupInfo> ConsumeGroupList { get; set; }

        public string Creator { get; set; }
        public long CreatedTime { get; set; }
        public long LastUpdatedTime { get; set; } 
    }

    public class ConsumeGroupInfo : ErrorInfo
    {
        public string TopicName { get; set; }
        public string GroupName { get; set; }
        public int Mask { get; set; }
        public string Filter { get; set; }
        public long MessageCount { get; set; }
        public int ConsumerCount { get; set; }
        public List<string> ConsumerList { get; set; }

        public string Creator { get; set; }
        public long CreatedTime { get; set; }
        public long LastUpdatedTime { get; set; }
    }
}
