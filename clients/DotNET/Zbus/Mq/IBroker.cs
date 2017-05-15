using System;
using Zbus.Mq.Net;

namespace Zbus.Mq
{
    public class ClientHint
    {
        public String Mq { get; set; }
        public String Broker { get; set; }
    }

    public interface IBroker : IMessageInvoker
    {
        IMessageInvoker GetInvoker(ClientHint hint);
        void CloseInvoker(IMessageInvoker invoker);
    }

    public delegate void BrokerHandler(IBroker broker);

}
