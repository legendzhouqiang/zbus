using System;
using System.Threading.Tasks;

namespace Zbus.Net
{
   public interface IClient<REQ, RES> : IInvoker<REQ, RES>, IPoolable
   { 
      Task SendAsync(REQ req);
      Task<RES> RecvAsync();

      V Attr<V>(string key);
      void Attr<V>(string key, V value);

      void StartHeartbeat(int heartbeatInterval);
      void StopHeartbeat();
      void Heartbeat();
   } 
}
