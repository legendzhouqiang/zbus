using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using zbus.Remoting;
namespace zbus
{
    public enum MessageMode
    {
        MQ = 1<<0,
        PubSub = 1<<1,
        Temp = 1<<2,
    }

    public class Proto
    {
        public static readonly string Produce     = "produce";    //生产消息
        public static readonly string Consume     = "consume";    //消费消息 
        public static readonly string Request     = "request";    //请求等待应答消息  
        public static readonly string Heartbeat   = "heartbeat";  //心跳消息
        public static readonly string Admin       = "admin";      //管理类消息
        public static readonly string CreateMQ    = "create_mq";      
        public static readonly string TrackReport = "track_report";
        public static readonly string TrackSub    = "track_sub";
        public static readonly string TrackPub    = "track_pub";


        public static Message BuildAdminMessage(string registerToken, string cmd, IDictionary<string, string> args)
        {
            Message msg = new Message();
            msg.Command = Proto.Admin;
            msg.Token = registerToken;
            msg.SetHead("cmd", cmd);
            foreach (KeyValuePair<string,string> kv in args)
            {
                msg.SetHead(kv.Key, kv.Value);
            }
            return msg;
        }
    }
}
