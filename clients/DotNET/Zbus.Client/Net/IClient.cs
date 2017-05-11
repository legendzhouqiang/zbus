namespace Zbus.Client.Net
{
   public interface IClient<REQ, RES> : IInvoker<REQ, RES>, IPoolable
   {  
      void Send(REQ req, int timeout = 10000); 
      RES Recv(int timeout = 10000);

      V Attr<V>(string key);
      void Attr<V>(string key, V value);

      void StartHeartbeat(int heartbeatInterval);
      void StopHeartbeat();
      void Heartbeat();
   } 
}
