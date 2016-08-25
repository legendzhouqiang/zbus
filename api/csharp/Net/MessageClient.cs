using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading;


using Zbus.Kit.Log;

namespace Zbus.Net
{
   public class MessageClient
   {
      private static readonly ILogger log = LoggerFactory.GetLogger(typeof(MessageClient));
       
      private Socket socket; 
      private string host = "127.0.0.1";
      private int port = 15555;
      private bool autoReconnect = true;
      private int reconnectInterval = 3000;
       
      private IoBuffer readBuf = new IoBuffer();
      private string msgidMatch = null;
      private IDictionary<string, Message> resultTable = new Dictionary<string, Message>();

      private DateTime timeCreated = DateTime.Now;
      private Thread heartbeator;
      public DateTime TimeCreated
      {
         get { return timeCreated; }
         set { timeCreated = value; }
      }

      public MessageClient(string address)
      {
         string[] blocks = address.Split(':');
         this.host = blocks[0];
         this.port = int.Parse(blocks[1]);
         heartbeator = new Thread(Heartbeat);
         heartbeator.Start();
      }

      private void Heartbeat()
      {
         while (Thread.CurrentThread.IsAlive)
         {
            if (IsConnected())
            {
               Message msg = new Message();
               msg.Cmd = "heartbeat";
               this.Send0(msg, 10000); 
            }
            Thread.Sleep(60000);
         }

      }

      public bool IsConnected()
      {
         if (socket == null) return false;
         return socket.Connected;
      }

      public void ConnectIfNeeded()
      {
         if (this.socket != null && this.socket.Connected) return; 
         while (true)
         {
            try
            {
               this.socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
               this.socket.Connect(this.host, this.port);  
               if (this.socket.Connected)
               {
                  break;
               }
               else
               {
                  this.socket = null;
                  continue;
               } 
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
      }

      public void Reconnect()
      {
         if (this.socket != null)
         {
            this.Close();
            this.socket = null;
         }
         while (this.socket == null)
         {

            try
            {
               log.DebugFormat("Trying reconnect to ({0}:{1})", this.host, this.port);
               ConnectIfNeeded();
               log.DebugFormat("Connected to ({0}:{1})", this.host, this.port);
            }
            catch (SocketException se)
            {
               this.socket = null;
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
         if (this.socket != null)
         { 
            this.socket.Close();
         }
         if (this.heartbeator != null)
         {
            this.heartbeator.Abort();
            this.heartbeator = null;
         }
      }

      private void MarkMessage(Message msg)
      {
         if (msg.Id == null)
         {
            msg.Id = System.Guid.NewGuid().ToString();
         }
         this.msgidMatch = msg.Id;
      }

      public void Send(Message msg, int timeout)
      { 
         this.MarkMessage(msg);
         Send0(msg, timeout);
      }

      private void Send0(Message msg, int timeout)
      {
         this.ConnectIfNeeded();
         if (log.IsDebugEnabled)
         {
            log.DebugFormat("Send: {0}", msg);
         }  
         IoBuffer buf = new IoBuffer();
         msg.Encode(buf);
         this.socket.SendTimeout = timeout;
         this.socket.Send(buf.Data, 0, buf.Position, SocketFlags.None);
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
         this.socket.ReceiveTimeout = timeout;

         while (true)
         {

            byte[] buf = new byte[4096];
            int n = this.socket.Receive(buf, 0, buf.Length, SocketFlags.None);
            this.readBuf.Put(buf, 0, n);

            IoBuffer tempBuf = this.readBuf.Duplicate();
            tempBuf.Flip(); //to read mode
            Message msg = Message.Decode(tempBuf);
            if (msg == null)
            {
               continue;
            }

            this.readBuf.Move(tempBuf.Position);

            if (this.msgidMatch != null && !this.msgidMatch.Equals(msg.Id))
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
