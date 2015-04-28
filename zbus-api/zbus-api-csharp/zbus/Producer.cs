using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using zbus.Remoting;


namespace zbus
{
    public class Producer
    {
        private string mq;
        private string token;
        private int mode;
        private RemotingClient client;
        public string Token
        {
            get { return this.token; }
            set { this.token = value; }
        }

        public Producer(RemotingClient client, string mq, params MessageMode[] mode)
        {
            this.client = client;
            this.mq = mq; 
            if (mode.Length == 0)
            {
                this.mode = (int)MessageMode.MQ;
            }
            else
            {
                this.mode = 0;
                foreach (MessageMode m in mode)
                {
                    this.mode |= (int)m;
                }
            }
        }

        public Message Send(Message msg, int timeout)
        {
            msg.Command = Proto.Produce;
            msg.Mq = this.mq;
            msg.Token = this.token;

            return this.client.Invoke(msg, timeout);
        }

        public static void Main_Producer(string[] args)
        {
            RemotingClient client = new RemotingClient("127.0.0.1:15555");

            Producer producer = new Producer(client, "MyMQ", MessageMode.MQ);
            
            Message msg = new Message();
            msg.Topic = "qhee";
            msg.SetBody("hello world from C# {0}", DateTime.Now);
            msg = producer.Send(msg, 10000); 

            Console.ReadKey();
        }
    }
}
