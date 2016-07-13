using System;
using Zbus.Broker;
namespace Zbus.Mq
{
   public class MqConfig : ICloneable
   {
      public IBroker Broker { get; set; }
      public string Mq { get; set; } 
      public string RegisterToken { get; set; } = "";
      public string AccessToken { get; set; } = ""; 
      public string Topic { get; set; }



      private int mode = (int)MqMode.MQ;
      public int Mode
      {
         get { return mode; }
      }


      public void SetMode(params MqMode[] modes)
      {
         foreach (MqMode m in modes)
         {
            this.mode |= (int)m;
         }
      }

      public object Clone()
      {
         return base.MemberwiseClone();
      }
   }
}
