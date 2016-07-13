
using Zbus.Mq;

namespace Zbus.RPC
{
   public class RpcConfig : MqConfig
   {
      public static readonly string DEFAULT_ENCODING = "UTF-8";
      public string Module { get; set; } = "";
      public int Timeout { get; set; } = 10000;
      public string Encoding { get; set; } = DEFAULT_ENCODING;
   }
}
