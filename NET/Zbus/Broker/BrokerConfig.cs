
using Zbus.Net;
namespace Zbus.Broker
{
   public class BrokerConfig
   {
      public string BrokerAddress { get; set; } = "127.0.0.1:15555";
      public int PoolSize { get; set; } = 32; 
   } 
}
