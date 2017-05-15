using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using Zbus.Net;

namespace Zbus.Net.Tcp
{
    public class Session : ISession
    {
        private TcpClient tcpClient;
        private ICodec codecRead;
        private ICodec codecWrite;
        private ByteBuffer readBuf = new ByteBuffer();

        private readonly string id = Guid.NewGuid().ToString();
        private IDictionary<string, object> attributes = null;
        private object attrLock = new object();
        public Session(TcpClient tcpClient, ICodec codecRead, ICodec codecWrite)
        {
            this.tcpClient = tcpClient;
            this.codecRead = codecRead;
            this.codecWrite = codecWrite;
        }

        public Session(TcpClient tcpClient, ICodec codec)
        {
            this.tcpClient = tcpClient;
            this.codecRead = this.codecWrite = codec;
        }

        private NetworkStream Stream
        {
            get { return this.tcpClient.GetStream(); }
        }

        public bool Active
        {
            get
            {
                return tcpClient.Connected;
            }
        }

        public string Id
        {
            get
            {
                return id;
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

        public V Attr<V>(string key)
        {
            initAttributesIfNeeded();
            object value = null;
            this.attributes.TryGetValue(key, out value);
            return (V)value;
        }

        public void Attr<V>(string key, V value)
        {
            initAttributesIfNeeded();
            this.attributes[key] = value;
        }

        private void initAttributesIfNeeded()
        {
            if (this.attributes == null)
            {
                lock (attrLock)
                {
                    if (this.attributes == null)
                    {
                        this.attributes = new ConcurrentDictionary<string, object>();
                    }
                }
            }
        }


        public void WriteAndFlush(object msg, int timeout = 10000)
        {
            Write(msg, timeout);
            Flush();
        }

        public void Write(object msg, int timeout = 10000)
        {
            Stream.WriteTimeout = timeout;

            ByteBuffer buf = this.codecWrite.Encode(msg);
            Stream.Write(buf.Data, 0, buf.Limit);
        }

        public void Flush()
        {
            Stream.Flush();
        }

        public object Read(int timeout = 10000)
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
                    return msg;
                }
                int n = Stream.Read(buf, 0, buf.Length);
                this.readBuf.Put(buf, 0, n);
            }
        }


        public void Dispose()
        {
            Stream.Close();
            this.tcpClient.Close();
        }
    }
}
