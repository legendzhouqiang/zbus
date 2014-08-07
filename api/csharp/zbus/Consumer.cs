using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;

using zbus.Remoting;

namespace zbus
{
    public class Consumer
    {
        private RemotingClient client;
        private string mq;
        private string accessToken;
        private string registerToken;
        private int mode;
        private int readTimeout = 3000;
        private bool autoRegister = true;
        private string topic = null;

        public string Topic
        {
            get { return this.topic; }
            set { this.topic = value; }
        }

        public string AccessToken
        {
            get { return this.accessToken; }
            set { this.accessToken = value; }
        }

        public string RegisterToken
        {
            get { return this.registerToken; }
            set { this.registerToken = value; }
        }


        public Consumer(RemotingClient client, string mq, params MessageMode[] mode)
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

        public bool Register()
        {
            IDictionary<string, string> args = new Dictionary<string, string>();
            args.Add("mq_name", mq);
    	    args.Add("access_token", accessToken);
            args.Add("mq_mode", "" + this.mode);

            Message req = Proto.BuildAdminMessage(registerToken, Proto.CreateMQ, args);
            Message res = this.client.Invoke(req, this.readTimeout);
            if (res == null)
            {
                return false;
            }
            return res.IsStatus200();

        }

        public Message Recv(int timeout)
        {
            Message req = new Message();
            req.Command = Proto.Consume;
            req.Mq = this.mq;
            req.Token = this.accessToken;
            if ((this.mode & (int)MessageMode.PubSub) != 0)
            {
                if (this.topic != null)
                {
                    req.Topic = this.topic;
                }
            }
            try { 

                Message res = this.client.Invoke(req, timeout);
                if (res != null && res.IsStatus404() && this.autoRegister)
                {
                    if (!this.Register())
                    {
                        throw new SystemException("register error");
                    }
                    return Recv(timeout);
                }

                return res;
            }
            catch (IOException ex)
            {
                if (Environment.Version.Major < 4) //.net 3.5 socket sucks!!!!
                {
                    this.HandleFailover();
                }
                else 
                {
                    if (!ex.Message.Contains("period of time")) //timeout just ignore
                    {
                        this.HandleFailover();
                    } 
                }
                
            } 

            return null;

        }

        public void Reply(Message msg, int timeout)
        {
            msg.SetHead(Message.HEADER_REPLY_CODE, msg.Status);
            msg.Command = Proto.Produce;
            msg.Ack = false;
            this.client.Send(msg, timeout);
        }


        private void HandleFailover()
        {
            this.client.Reconnect();
        }

        public static void Main_Consumer(string[] args)
        {
            RemotingClient client = new RemotingClient("127.0.0.1:15555");
            Consumer csm = new Consumer(client, "MySub", MessageMode.PubSub);
            csm.Topic = "qhee";
            while (true)
            {
                Message msg = csm.Recv(10000);
                if (msg == null) continue;
                Console.WriteLine(msg);
            } 
 
        }
    }
}
