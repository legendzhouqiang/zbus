
using Zbus.Mq;
using Zbus.Broker;
namespace Zbus.RPC
{
    public class ServiceConfig : MqConfig
    {
        private IMessageProcessor messageProcessor;
        private int consumerCount = 1;
        private int readTimeout = 3000; //30ms
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

        public IMessageProcessor MessageProcessor
        {
            get { return messageProcessor; }
            set { messageProcessor = value; }
        }

        public int ConsumerCount
        {
            get { return consumerCount; }
            set { consumerCount = value; }
        }

        public int ReadTimeout
        {
            get { return readTimeout; }
            set { readTimeout = value; }
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
