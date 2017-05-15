using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Client.Broker; 

namespace Zbus.Client.Mq
{
   public enum MqMode
   {
      MQ = 1 << 0,
      Memory = 1 << 2,
      RPC = 1 << 3,
   } 

   public class MqException : Exception
   {
      public MqException()
      {
      }

      public MqException(string message)
          : base(message)
      {
      }
      public MqException(string message, Exception inner)
          : base(message, inner)
      {
      }
   }

   public class MqConfig : ICloneable
   {
      public IBroker Broker { get; set; }
      public string Mq { get; set; }
      public int Mode { get; set; } = (int)MqMode.MQ; 
      public string AppId { get; set; } = ""; 
      public string Token { get; set; } = "";   
      public string Topic { get; set; }


      public void SetMode(params MqMode[] modes)
      {
         foreach (MqMode m in modes)
         {
            this.Mode |= (int)m;
         }
      }

      public object Clone()
      {
         return base.MemberwiseClone();
      }
   }

   public class MqAdmin
   {
      protected readonly IBroker broker;
      protected String mq;          
      protected int mode; 

      public MqAdmin(IBroker broker, String mq, params MqMode[] modes)
      {
         this.broker = broker;
         this.mq = mq;
         if (modes.Length == 0)
         {
            this.mode = (int)MqMode.MQ;
         }
         else
         {
            foreach (MqMode m in modes)
            {
               this.mode |= (int)m;
            }
         }
      }

      public MqAdmin(MqConfig config)
      {
         this.broker = config.Broker;
         this.mq = config.Mq;
         this.mode = config.Mode;
      }
       
      public bool CreateMq(int timeout=10000)
      { 
         Message req = new Message();
         req.Cmd = Protocol.CreateMQ;
         req.SetHead("mq_name", mq);
         req.SetHead("mq_mode", "" + mode);

         Message res = Invoke(req, timeout);
         if (res == null)
         {
            return false;
         }
         return res.Status == "200";
      } 

      protected virtual Message Invoke(Message req, int timeout=10000)
      { 
         return broker.Invoke(req, timeout);
      }

      protected ClientHint GetClientHint()
      {
         ClientHint hint = new ClientHint();
         hint.Mq = mq;
         return hint;
      }
   } 
}
