using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net.Sockets;
using System.Threading;

using System.Text.RegularExpressions;
using zbus.Logging;

namespace zbus.Remoting
{
    public class RemotingClient
    {
        private static readonly ILogger log = LoggerFactory.GetLogger(typeof(RemotingClient));

        private TcpClient client;
        private string host = "127.0.0.1";
        private int port = 15555;
        private bool autoReconnect = true;
        private int reconnectInterval = 3000;

        private Stream stream; 
        private IoBuffer readBuf = new IoBuffer();
        private string msgidMatch = null;
        private IDictionary<string, Message> resultTable = new Dictionary<string, Message>();

        public RemotingClient(string address)
        {
            string[] blocks = address.Split(':');
            this.host = blocks[0];
            this.port = int.Parse(blocks[1]);
        }

        public void ConnectIfNeeded()
        {
            if (this.client != null) return;
            while (true) {
                try
                {
                    this.client = new TcpClient(this.host, this.port);
                    break;
                }
                catch (SocketException se)
                {
                    log.Error(se.Message, se);
                    if (this.autoReconnect)
                    {
                        log.DebugFormat("Failed to connecting {0}:{1}, try again in 3s", this.host, this.port);
                        Thread.Sleep(this.reconnectInterval);
                        continue;
                    }
                    else
                    {
                        throw se;
                    }  
                }
            }
            log.DebugFormat("Connected to {0}:{1}", this.host, this.port);
            this.stream = this.client.GetStream();
        }

        public void Reconnect()
        {
            if (this.client != null)
            {
                this.Close();
                this.client = null;
            }
            while (this.client == null)
            {

                try
                {
                    log.DebugFormat("Trying reconnect to ({0}:{1})", this.host, this.port);
                    ConnectIfNeeded();
                    log.DebugFormat("Connected to ({0}:{1})", this.host, this.port);
                }
                catch (SocketException se)
                {
                    this.client = null;
                    log.Error(se.Message, se);
                    if (this.autoReconnect)
                    {
                        Thread.Sleep(this.reconnectInterval);
                        continue;
                    }
                    else
                    {
                        throw se;
                    }
                }
            }

        }

        public void Close()
        {
            if (this.client != null)
            {
                this.stream.Close();
                this.client.Close();
            }
        }

        private void MarkMessage(Message msg)
        {
            if (msg.MsgId == null)
            {
                msg.MsgId = System.Guid.NewGuid().ToString();
            }
            this.msgidMatch = msg.MsgId;
        }

        public void Send(Message msg, int timeout)
        {
            this.ConnectIfNeeded();
            if (log.IsDebugEnabled)
            {
                log.DebugFormat("Send: {0}", msg);
            }
            this.MarkMessage(msg);

            IoBuffer buf = new IoBuffer();
            msg.Encode(buf);
            this.stream.WriteTimeout = timeout;
            this.stream.Write(buf.Data, 0, buf.Position);
        }

        public Message Recv(int timeout)
        {
            this.ConnectIfNeeded(); 
            if (this.msgidMatch != null && this.resultTable.ContainsKey(this.msgidMatch))
            {
                Message msg = this.resultTable[this.msgidMatch];
                this.resultTable.Remove(this.msgidMatch);
                return msg;
            }
            this.client.ReceiveTimeout = timeout;

            while (true)
            {

                byte[] buf = new byte[4096];
                int n = this.stream.Read(buf, 0, buf.Length); 
                this.readBuf.Put(buf, 0, n);

                IoBuffer tempBuf = this.readBuf.Duplicate();
                tempBuf.Flip(); //to read mode
                Message msg = Message.Decode(tempBuf);
                if (msg == null)
                {
                    continue;
                }

                this.readBuf.Move(tempBuf.Position);

                if (this.msgidMatch != null && !this.msgidMatch.Equals(msg.MsgId))
                {
                    this.resultTable[this.msgidMatch] = msg;
                    continue;
                }

                if (log.IsDebugEnabled)
                {
                    log.DebugFormat("Recv: {0}", msg);
                }
                return msg;
            } 
        }

        public Message Invoke(Message msg, int timeout)
        {
            this.Send(msg, timeout);
            return this.Recv(timeout);
        } 
      
    }
}
