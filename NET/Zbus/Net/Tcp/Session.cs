using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;

namespace Zbus.Net.Tcp
{
   public class Session : ISession
   {
      private TcpClient client;
      private ICodec codecRead;
      private ICodec codecWrite;
      private IoBuffer readBuf = new IoBuffer();


      private readonly string id = Guid.NewGuid().ToString();
      private IDictionary<string, object> attributes = null;
      private object attrLock = new object();
      public Session(TcpClient client, ICodec codecRead, ICodec codecWrite)
      {
         this.client = client;
         this.codecRead = codecRead;
         this.codecWrite = codecWrite;
      }

      public Session(TcpClient client, ICodec codec)
      {
         this.client = client;
         this.codecRead = this.codecWrite = codec;
      }

      public TcpClient Client
      {
         get { return client; }
      }

      private NetworkStream Stream
      {
         get { return this.client.GetStream(); }
      }

      public bool Active
      {
         get
         {
            return client.Connected;
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
            return ((IPEndPoint)client.Client.LocalEndPoint).Address.ToString();
         }
      }

      public string RemoteAddress
      {
         get
         {
            return ((IPEndPoint)client.Client.RemoteEndPoint).Address.ToString();
         }
      }

      public V Attr<V>(string key)
      {
         InitAttributesIfNeeded();
         object value = null;
         this.attributes.TryGetValue(key, out value);
         return (V)value;
      }

      public void Attr<V>(string key, V value)
      {
         InitAttributesIfNeeded();
         this.attributes[key] = value;
      }

      private void InitAttributesIfNeeded()
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

      public async Task<object> ReadAsync()
      {
         byte[] buf = new byte[4096];
         while (true)
         {
            IoBuffer tempBuf = this.readBuf.Duplicate();
            tempBuf.Flip(); //to read mode
            object msg = codecRead.Decode(tempBuf);
            if (msg != null)
            {
               this.readBuf.Move(tempBuf.Position);
               return msg;
            }
            int n = await Stream.ReadAsync(buf, 0, buf.Length);
            this.readBuf.Put(buf, 0, n); 
         } 
      }

      public Task FlushAsync()
      {
         return Stream.FlushAsync(); 
      }

      public async Task WriteAndFlushAsync(object msg)
      {
         await WriteAsync(msg);
         await FlushAsync();
      }

      public Task WriteAsync(object msg)
      {
         IoBuffer buf = this.codecWrite.Encode(msg); 
         return Stream.WriteAsync(buf.Data, 0, buf.Limit); 
      }

      public void Dispose()
      {
         this.client.Close();
      }
   }
}
