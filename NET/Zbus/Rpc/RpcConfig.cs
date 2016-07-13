
using Zbus.Mq;

namespace Zbus.RPC
{
   public class RpcConfig : MqConfig
   {
      public static readonly string DEFAULT_ENCODING = "UTF-8";
      private string module = "";
      private int timeout = 10000;
      private string encoding = DEFAULT_ENCODING;

      public string Module
      {
         get { return module; }
         set { module = value; }
      }

      public int Timeout
      {
         get { return timeout; }
         set { timeout = value; }
      }

      public string Encoding
      {
         get { return encoding; }
         set { encoding = value; }
      }
   }
}
