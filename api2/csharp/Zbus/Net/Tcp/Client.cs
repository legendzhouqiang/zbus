using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Net.Sockets;
using System.Threading.Tasks;
using System.Threading; 

namespace Zbus.Net.Tcp
{
   
   public class Client<REQ, RES> : IClient<REQ, RES> 
      where REQ: IId 
      where RES: IId
   {   
      private Session session;  
      private IDictionary<string, RES> resultTable = new ConcurrentDictionary<string, RES>();  

      public Client(ICodec codecRead, ICodec codecWrite)
      {
         this.session = new Session(new TcpClient(), codecRead, codecWrite);
      }
      public Client(ICodec codec)
      {  
         this.session = new Session(new TcpClient(), codec);
      }

      public Task ConnectAsync(string host, int port)
      {
         return session.Client.ConnectAsync(host, port);
      }

      public Task ConnectAsync(string address)
      {
         string[] bb = address.Trim().Split(':');
         if(bb.Length != 2)
         {
            throw new ArgumentException("Address format:<host>:<port>, but with " + address);
         }
         return ConnectAsync(bb[0], int.Parse(bb[1]));
      }


      public bool Active
      {
         get
         {
            return session.Active;
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
        
      public async Task<RES> InvokeAsync(REQ req)
      {
         await SendAsync(req);
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

            res = await RecvAsync();
            if (res.Id == reqId) return res; 

            resultTable[res.Id] = res;
         }   
      }

      public RES InvokeSync(REQ req)
      {
         return InvokeSync(req, 10000);
      }

      public RES InvokeSync(REQ req, int timeout)
      {
         Task<RES> task = InvokeAsync(req); 
         task.Wait(timeout);
         return task.Result; 
      }

      public Task SendAsync(REQ req)
      {
         if (req.Id == null)
         {
            req.Id = Guid.NewGuid().ToString();
         }
         return session.WriteAndFlushAsync(req); 
      }

      public async Task<RES> RecvAsync()
      {
         object msg = await session.ReadAsync(); 
         RES res =  (RES)msg; 
         return res;
      }

      private Thread heartbeatThread;
      private CancellationTokenSource heartbeatCTS;
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
         session.Dispose();
         session = null;
      } 
   }


   public class Client<T> : Client<T, T> where T : IId
   {
      public Client(ICodec codecRead, ICodec codecWrite) : base(codecRead, codecWrite) { }
      public Client(ICodec codec) : base(codec) { }
   }
}
