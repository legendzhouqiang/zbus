
namespace Zbus.Client.Net
{
   /// <summary>
   /// ICodec serialize or deserialize between domain object and binary data
   /// </summary>
   public interface ICodec
   {
      /// <summary>
      /// Encode the msg object to binary data 
      /// </summary>
      /// <param name="msg">domain object to serialize on wire</param>
      /// <returns>IoBuffer ready to read, encoder should be responsible to flip IoBuffer</returns>
      ByteBuffer Encode(object msg);
      
      /// <summary>
      /// Decode the buffer to domain object 
      /// </summary>
      /// <param name="buf">ByteBuffer read from</param>
      /// <returns>Decoded object or null if no ready</returns>
      object Decode(ByteBuffer buf);
   }
}
