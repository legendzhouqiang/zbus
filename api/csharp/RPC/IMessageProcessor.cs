
using Zbus.Net;
namespace Zbus.RPC
{
    public interface IMessageProcessor
    {
        Message Process(Message request);
    }

}
