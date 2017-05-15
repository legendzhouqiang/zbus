using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Mq;
using Zbus.Mq.Net;

namespace Zbus.Mq
{
    public class ZbusBroker : IBroker
    {
        private IBroker support;
        public ZbusBroker() :
           this(new BrokerConfig())
        {

        }
        public ZbusBroker(string address) :
           this(new BrokerConfig
           {
               BrokerAddress = address,
           })
        {

        }

        public ZbusBroker(BrokerConfig config)
        {
            bool ha = false;
            string addr = config.BrokerAddress;
            if (addr.StartsWith("["))
            {
                if (addr.EndsWith("]"))
                {
                    addr = addr.Substring(1, addr.Length - 2);
                }
                else
                {
                    throw new ArgumentException(addr + " is invalid");
                }
            }
            if (addr.Contains(',') || addr.Contains(' ') || addr.Contains(';'))
            {
                ha = true;
            }
            if (ha)
            {
                throw new ArgumentException("HA not support");
            }
            else
            {
                support = new SingleBroker(config);
            }
        }


        public void CloseInvoker(IMessageInvoker invoker)
        {
            support.CloseInvoker(invoker);
        }

        public void Dispose()
        {
            support.Dispose();
        }

        public IMessageInvoker GetInvoker(ClientHint hint)
        {
            return support.GetInvoker(hint);
        }

        public Message Invoke(Message req, int timeout = 10000)
        {
            return support.Invoke(req, timeout);
        }
    }
}
