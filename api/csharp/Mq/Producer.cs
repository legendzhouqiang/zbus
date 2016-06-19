using System;

using Zbus.Net;
using Zbus.Broker;
namespace Zbus.Mq
{
    public class Producer: MqAdmin
    {
        public Producer(IBroker broker, String mq, params MqMode[] modes)
            :base(broker, mq, modes)
        {
        }

        public Producer(MqConfig config)
            :base(config)
        {
        }
      

        public Message Send(Message msg, int timeout)
        {
            msg.Cmd = Proto.Produce;
            msg.Mq = this.mq; 

            return this.broker.InvokeSync(msg, timeout);
        }
    }
}
