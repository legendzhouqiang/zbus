using System;
using System.Threading.Tasks;
using Zbus.Net;

namespace Zbus.Mq.Net
{
   
   public class MessageClient : Client<Message>
   {
      public MessageClient(string serverAddress): base(serverAddress, new MessageCodec())
      { 
      } 
   } 
}
