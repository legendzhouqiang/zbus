
using Zbus.Mq;
using Zbus.Broker;
namespace Zbus.RPC
{
   public class ServiceConfig : MqConfig
   {
      public IMessageProcessor MessageProcessor { get; set; }
      public int ConsumerCount { get; set; } = 1;
      public int ReadTimeout { get; set; } = 3000; //30ms
      private IBroker[] brokers;

      public ServiceConfig(params IBroker[] brokers)
      {
         this.SetMode(MqMode.RPC);
         this.brokers = brokers;
         if (this.brokers.Length > 0)
         {
            this.Broker = this.brokers[0];
         }
      } 

      public IBroker[] Brokers
      {
         get
         {
            if (this.brokers == null || this.brokers.Length == 0)
            {
               if (this.Broker != null)
               {
                  this.brokers = new IBroker[] { this.Broker };
               }
            }
            return this.brokers;
         }
      }
   }

}
