using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using zbus.Remoting;


namespace zbus
{
    public class Caller
    {
        private RemotingClient client;

        private string mq;
        private string token; 
        
        public string Token
        {
            get { return this.token; }
            set { this.token = value; }
        }

        public Caller(RemotingClient client, string mq)
        {
            this.client = client;
            this.mq = mq;  
        }

        public Message Invoke(Message msg, int timeout)
        {
            msg.Command = Proto.Request;
            msg.Mq = this.mq;
            msg.Token = this.token; 

            return this.client.Invoke(msg, timeout);
        }

        public static void Main(string[] args)
        {
            RemotingClient client = new RemotingClient("127.0.0.1:15555");
            Caller rpc = new Caller(client, "MyService");
            
            Message msg = new Message(); 
            msg.SetBody("request from C# {0}", DateTime.Now);
            msg = rpc.Invoke(msg, 10000);

            Console.WriteLine(msg);
            Console.ReadKey();
        }
    }
}
