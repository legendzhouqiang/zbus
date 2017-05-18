using System;
using System.Threading;
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

        private async Task<T> InvokeObjectAsync<T>(Message msg, CancellationToken? token = null) where T: ErrorInfo, new()
        {
            msg.Token = this.Token;
            if (token == null)
            {
                token = CancellationToken.None;
            }
            Message res = await this.InvokeAsync(msg, token.Value);
            if (res.Status != "200")
            {
                T info = new T();
                info.Error = res.BodyString;
                return info;
            }
            return ConvertKit.DeserializeObject<T>(res.BodyString);
        }

        public async Task<ServerInfo> QueryServerAsync(CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
            }; 
            return await InvokeObjectAsync<ServerInfo>(msg, token);
        }

        public async Task<TopicInfo> QueryTopicAsync(string topic, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
            };
            return await InvokeObjectAsync<TopicInfo>(msg, token);
        }

        public async Task<ConsumeGroupInfo> QueryGroupAsync(string topic, string group, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.QUERY,
                Topic = topic,
                ConsumeGroup = group,
           
            };
            return await InvokeObjectAsync<ConsumeGroupInfo>(msg, token);
        }

        public async Task<TopicInfo> DeclareTopicAsync(string topic, int? topicMask =null, CancellationToken? token = null)
        {
            Message msg = new Message
            {
                Cmd = Protocol.DECLARE,
                Topic = topic, 
                TopicMask = topicMask,
            };
            return await InvokeObjectAsync<TopicInfo>(msg, token);
        }
    }
}
