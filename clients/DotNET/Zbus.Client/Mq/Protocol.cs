using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Client.Broker;

namespace Zbus.Client.Mq
{
    public class Protocol
    {
        public static readonly string Produce = "produce";
        public static readonly string Consume = "consume";
        public static readonly string Route = "route";
        public static readonly string Heartbeat = "heartbeat";
        public static readonly string CreateMQ = "create_mq";
    }
}
