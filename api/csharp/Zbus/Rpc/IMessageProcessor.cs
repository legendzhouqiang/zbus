
using Zbus.Net;
using Zbus.Net.Http;

namespace Zbus.RPC
{
   public interface IMessageProcessor
   {
      Message Process(Message request);
   }
}
