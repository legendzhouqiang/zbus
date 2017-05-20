using log4net;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace Zbus.Mq
{
    public class ConsumeThread
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(ConsumeThread));

        public Func<MqClient> ClientFactory { get; set; }
        public Action<Message, MqClient> MessageHandler { get; set; }
        public int ThreadCount { get; set; }
        public bool MessageHandleInPool { get; set; }
        public int ConsumeTimeout { get; set; }
        public int? ConsumeWindow { get; set; } 

        public string Topic { get; private set; }
        public string Token { get; set; }
        public ConsumeGroup ConsumeGroup { get; private set; } 
         

        private CancellationTokenSource cts = new CancellationTokenSource(); 
        private Thread[] consumeThreadList;
        private MqClient[] clients;

        public ConsumeThread(string topic, ConsumeGroup group = null)
        {
            ConsumeTimeout = 10000; //10s 
            Topic = topic;
            ConsumeGroup = group==null? new ConsumeGroup(topic): group;
            MessageHandleInPool = false;
            ThreadCount = 1; 
        }

        private async Task<Message> TakeAsync(MqClient client, CancellationToken? token=null)
        {
            Message res = await client.ConsumeAsync(Topic, ConsumeGroup.GroupName, ConsumeWindow, token);
            if (res == null) return res;
            if(res.Status == "404")
            {
                await client.DeclareGroupAsync(Topic, ConsumeGroup, token);
                return await TakeAsync(client, token);
            }

            if(res.Status == "200")
            {
                return res;
            }
            throw new MqException(res.BodyString); 
        }

        public void Start()
        {
            if (MessageHandler == null)
            {
                throw new InvalidOperationException("Missing MessageHandler");
            }
            if (ClientFactory == null)
            {
                throw new InvalidOperationException("Missing ClientFactory");
            }

            lock (this)
            {
                if (this.consumeThreadList != null) return;
            }
            this.consumeThreadList = new Thread[ThreadCount];
            this.clients = new MqClient[ThreadCount];

            for(int i = 0; i < this.consumeThreadList.Length; i++)
            {
                MqClient client = this.clients[i] = ClientFactory();
                this.consumeThreadList[i] = new Thread(async () =>
                {
                    using (client) { 
                        while (!cts.IsCancellationRequested)
                        {
                            Message msg;
                            try
                            {
                                msg = await TakeAsync(client, cts.Token);
                                if (msg == null) continue;
                                if (MessageHandleInPool)
                                {
                                    await Task.Run(() =>
                                    {
                                        MessageHandler(msg, client);
                                    });
                                }
                                else
                                {
                                    MessageHandler(msg, client);
                                }
                            }
                            catch (SocketException e)
                            {
                                Console.WriteLine(e);
                                log.Error(e);
                                Thread.Sleep(3000);
                            }
                            catch (Exception e)
                            {
                                Console.WriteLine(e);
                                log.Error(e);
                            }
                        }
                    }
                });
            } 
            foreach(Thread thread in this.consumeThreadList)
            {
                thread.Start();
            }
        }

        public void Stop()
        {  
            cts.Cancel();
            for (int i = 0; i < this.clients.Length; i++)
            {
                try
                {
                    this.clients[i].Dispose();
                }
                catch(Exception e)
                {
                    log.Error(e.Message, e);
                    //ignore
                }
            }
        } 
    } 
}
