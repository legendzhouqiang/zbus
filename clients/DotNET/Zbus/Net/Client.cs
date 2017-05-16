using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using Zbus.Net;

namespace Zbus.Net
{

    /// <summary>
    /// Identity interface to track message match for asynchroneous invocation.
    /// </summary>
    public interface IId
    {
        /// <summary>
        /// Identity string
        /// </summary>
        string Id { get; set; }
    }

    public class Client<REQ, RES> : IDisposable  where REQ : IId where RES : IId
    { 
        private readonly TcpClient tcpClient;
        private ICodec codecRead;
        private ICodec codecWrite;
        private string serverAddress;
        private ByteBuffer readBuf = new ByteBuffer(); 
        private IDictionary<string, RES> resultTable = new ConcurrentDictionary<string, RES>(); 

        public Client(string serverAddress, ICodec codecRead, ICodec codecWrite)
        {
            this.serverAddress = serverAddress;
            tcpClient = new TcpClient();
            tcpClient.NoDelay = true;
            this.codecRead = codecRead;
            this.codecWrite = codecWrite;
        }


        public Client(string serverAddress, ICodec codec) : this(serverAddress, codec, codec)
        {
        }

        public void Connect()
        {
            string[] bb = this.serverAddress.Trim().Split(':');
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
            this.tcpClient.Connect(host, port);
        }

        public bool Active
        {
            get
            {
                return this.tcpClient != null && this.tcpClient.Connected;
            }
        }
        public string LocalAddress
        {
            get
            {
                return ((IPEndPoint)tcpClient.Client.LocalEndPoint).Address.ToString();
            }
        }

        public string RemoteAddress
        {
            get
            {
                return ((IPEndPoint)tcpClient.Client.RemoteEndPoint).Address.ToString();
            }
        }

        private NetworkStream Stream
        {
            get { return this.tcpClient.GetStream(); }
        }

        public RES Invoke(REQ req, int timeout = 3000)
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

        public void Send(REQ req, int timeout = 3000)
        {
            if (req.Id == null)
            {
                req.Id = Guid.NewGuid().ToString();
            }
            Stream.WriteTimeout = timeout; 
            ByteBuffer buf = this.codecWrite.Encode(req);
            Stream.Write(buf.Data, 0, buf.Limit);
            Stream.Flush();
        }

        public RES Recv(int timeout = 3000)
        {
            Stream.ReadTimeout = timeout;

            byte[] buf = new byte[4096];
            while (true)
            {
                ByteBuffer tempBuf = this.readBuf.Duplicate();
                tempBuf.Flip(); //to read mode
                object msg = codecRead.Decode(tempBuf);
                if (msg != null)
                {
                    this.readBuf.Move(tempBuf.Position);
                    RES res = (RES)msg;
                    return res;
                }
                int n = Stream.Read(buf, 0, buf.Length);
                this.readBuf.Put(buf, 0, n);
            } 
        } 

        public void Dispose()
        { 
            if(Stream != null)
            {
                Stream.Close();
            }
            if(this.tcpClient != null)
            {
                this.tcpClient.Close();
            }
        }
    }


    public class Client<T> : Client<T, T> where T : IId
    {
       public Client(string serverAddress, ICodec codec) : base(serverAddress, codec) { }
    }
}
