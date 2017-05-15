using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net.Sockets; 
using System.Threading;
using Zbus.Net;

namespace Zbus.Net.Tcp
{

    public class Client<REQ, RES> : IClient<REQ, RES>
       where REQ : IId
       where RES : IId
    {
        private ISession session;
        private readonly TcpClient tcpClient;
        private IDictionary<string, RES> resultTable = new ConcurrentDictionary<string, RES>();
        private Thread heartbeatThread;
        private CancellationTokenSource heartbeatCTS;

        public Client(ICodec codecRead, ICodec codecWrite)
        {
            tcpClient = new TcpClient();
            tcpClient.NoDelay = true;
            this.session = new Session(tcpClient, codecRead, codecWrite);
        }


        public Client(ICodec codec) : this(codec, codec)
        {
        }

        public void Connect(string host, int port)
        {
            tcpClient.Connect(host, port);
        }

        public void Connect(string address)
        {
            string[] bb = address.Trim().Split(':');
            string host;
            int port = 80;
            if (bb.Length < 2)
            {
                host = bb[0];
            }
            else
            {
                host = bb[0];
                port = int.Parse(bb[1]);
            }
            Connect(host, port);
        }

        public bool Active
        {
            get
            {
                return session != null && session.Active;
            }
        }

        public V Attr<V>(string key)
        {
            return session.Attr<V>(key);
        }

        public void Attr<V>(string key, V value)
        {
            session.Attr<V>(key, value);
        }

        public RES Invoke(REQ req, int timeout = 10000)
        {
            Send(req, timeout);
            string reqId = req.Id;
            RES res;
            while (true)
            {
                if (resultTable.ContainsKey(reqId))
                {
                    res = resultTable[reqId];
                    resultTable.Remove(reqId);
                    return res;
                }

                res = Recv(timeout);
                if (res.Id == reqId) return res;

                resultTable[res.Id] = res;
            }
        }

        public void Send(REQ req, int timeout = 10000)
        {
            if (req.Id == null)
            {
                req.Id = Guid.NewGuid().ToString();
            }
            session.WriteAndFlush(req, timeout);
        }

        public RES Recv(int timeout = 10000)
        {
            object msg = session.Read(timeout);
            RES res = (RES)msg;
            return res;
        }

        public void StartHeartbeat(int heartbeatInterval)
        {
            if (heartbeatThread != null) return;

            heartbeatCTS = new CancellationTokenSource();

            heartbeatThread = new Thread(() =>
            {
                while (!heartbeatCTS.Token.IsCancellationRequested)
                {
                    Thread.Sleep(heartbeatInterval);
                    Heartbeat();
                }
            });

            heartbeatThread.Start();
        }

        public void StopHeartbeat()
        {
            if (heartbeatThread == null) return;
            heartbeatCTS.Cancel();
            heartbeatThread.Abort();
            heartbeatThread = null;
        }

        public virtual void Heartbeat()
        {

        }

        public void Dispose()
        {
            StopHeartbeat();
            if (session != null)
            {
                session.Dispose();
            }
        }
    }


    public class Client<T> : Client<T, T> where T : IId
    {
        public Client(ICodec codecRead, ICodec codecWrite) : base(codecRead, codecWrite) { }
        public Client(ICodec codec) : base(codec) { }
    }
}
