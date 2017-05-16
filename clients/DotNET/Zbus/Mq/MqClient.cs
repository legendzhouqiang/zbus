using System;
using System.Threading.Tasks;
using Zbus.Mq.Net;

namespace Zbus.Mq
{
   
    public class MqClient : MessageClient
    {
        public string Token { get; set; } 
        public MqClient(string serverAddress, string certFile = null)
            : base(serverAddress, certFile)
        {
        }
        public MqClient(ServerAddress serverAddress, string certFile = null)
            : base(serverAddress, certFile)
        {
        }

        private T InvokeObject<T>(Message msg, int timeout = 3000) where T: ErrorInfo, new()
        {
            msg.Token = this.Token;

            Message res = this.Invoke(msg, timeout);
            if (res.Status != "200")
            {
                T info = new T();
                info.Error = res.BodyString;
                return info;
            }
            return ConvertKit.DeserializeObject<T>(res.BodyString);
        }

        public ServerInfo QueryServer(int timeout=3000)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
            }; 
            return InvokeObject<ServerInfo>(msg, timeout);
        }

        public TopicInfo QueryTopic(string topic, int timeout = 3000)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
            };
            return InvokeObject<TopicInfo>(msg, timeout);
        }

        public ConsumeGroupInfo QueryGroup(string topic, string group, int timeout = 3000)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
                ConsumeGroup = group,
           
            };
            return InvokeObject<ConsumeGroupInfo>(msg, timeout);
        } 
    }
}
