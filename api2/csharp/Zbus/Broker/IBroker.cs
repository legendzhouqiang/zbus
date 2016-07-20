using System;
using System.Threading.Tasks;
using Zbus.Net;
using Zbus.Net.Http;

namespace Zbus.Broker
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
}
