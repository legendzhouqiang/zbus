using System;
using Zbus.Broker;
namespace Zbus.Mq
{
   public class MqConfig : ICloneable
   {
      private IBroker broker;
      private string mq;
      private int mode = (int)MqMode.MQ;
      private string registerToken = "";
      private string accessToken = "";

      private string topic = null;


      public IBroker Broker
      {
         get { return broker; }
         set { broker = value; }
      }

      public string Mq
      {
         get { return mq; }
         set { mq = value; }
      }


      public int Mode
      {
         get { return mode; }
         set { mode = value; }
      }

      public string Topic
      {
         get { return topic; }
         set { topic = value; }
      }

      public string RegisterToken
      {
         get { return registerToken; }
         set { registerToken = value; }
      }

      public string AccessToken
      {
         get { return accessToken; }
         set { accessToken = value; }
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
