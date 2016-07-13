using System;
using System.Threading.Tasks;

namespace Zbus.Net
{
   public interface ISession : IDisposable
   {
      string Id { get; }
      string RemoteAddress { get; }
      string LocalAddress { get;  }
      bool Active { get; }

      Task WriteAsync(object msg);
      Task FlushAsync();
      Task WriteAndFlushAsync(object msg);   

      V Attr<V>(string key);
      void Attr<V>(string key, V value);
   }
}
