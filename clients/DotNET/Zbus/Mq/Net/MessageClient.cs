using System;
using System.Threading.Tasks;
using Zbus.Net;
using Zbus.Net.Tcp;

namespace Zbus.Mq.Net
{
   
   public class MessageClient : Client<Message>
   {
      public MessageClient(): base(new MessageCodec())
      { 
      }

      public override void Heartbeat()
      {
         Message msg = new Message();
         msg.Cmd = Message.HEARTBEAT;
         Send(msg);
      }
   } 
}
