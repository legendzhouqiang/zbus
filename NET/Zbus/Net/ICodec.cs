
namespace Zbus.Net
{
   public interface ICodec
   {
      /// <summary>
      /// Encode the msg object to binary data
      /// Note: IoBuffer encoded is assumed to be ready to read
      /// </summary>
      /// <param name="msg">domain object to serialize on wire</param>
      /// <returns></returns>
      IoBuffer Encode(object msg);
      /// <summary>
      /// Decode the buffer to domain object
      /// Note: IoBuffer after decoding is changed for position
      /// </summary>
      /// <param name="buf"></param>
      /// <returns></returns>
      object Decode(IoBuffer buf);
   }
}
