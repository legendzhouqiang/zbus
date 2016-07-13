
namespace Zbus.Net
{
   public interface IServer
   {
      void Start(int port, IServerAdaptor adaptor);
      void Start(string host, int port, IServerAdaptor adaptor);
      void Join();
   }
}
