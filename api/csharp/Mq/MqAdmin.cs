using System;

using Zbus.Broker;
using Zbus.Net;

namespace Zbus.Mq
{ 
    public class MqAdmin{     
	    protected readonly IBroker broker;      
	    protected String mq;                  //队列唯一性标识 
	    protected int mode; 
	    protected int invokeTimeout = 2500;


        public MqAdmin(IBroker broker, String mq,  params MqMode[] modes){
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

        public bool CreateMQ()
        {

            Message req = new Message();
            req.Cmd = Proto.CreateMQ;
            req.SetHead("mq_name", mq);
            req.SetHead("mq_mode", "" + mode);

            Message res = InvokeCreateMQ(req);
            if (res == null)
            {
                return false;
            }
            return res.IsStatus200();
        }

        protected Message InvokeCreateMQ(Message req)
        {
            return broker.InvokeSync(req, invokeTimeout);
        }

        protected ClientHint GetClientHint()
        {
            ClientHint hint = new ClientHint();
            hint.Mq = mq;
            return hint;
        }
    }
	
}
