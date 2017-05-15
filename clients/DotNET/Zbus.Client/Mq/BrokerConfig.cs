
using Zbus.Client.Net;
namespace Zbus.Client.Mq
{
    public class BrokerConfig
    {
        public string BrokerAddress { get; set; } = "127.0.0.1:15555";
        public int PoolSize { get; set; } = 32;
        public int HeartbeatInterval { get; set; } = 60000;
    }
}
