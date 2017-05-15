using System;
 
using Zbus.Client.Broker;
using System.Threading;
using System.Threading.Tasks;

namespace Zbus.Client.Mq
{
   public class Producer : MqAdmin
   {
      public Producer(IBroker broker, String mq, params MqMode[] modes)
          : base(broker, mq, modes)
      {
      }

      public Producer(MqConfig config)
          : base(config)
      {
      }
       
      public Message Send(Message msg, int timeout=10000)
      {
         msg.Cmd = Protocol.Produce;
         msg.Mq = this.mq; 

         return this.broker.Invoke(msg, timeout);
      }
   }
}
