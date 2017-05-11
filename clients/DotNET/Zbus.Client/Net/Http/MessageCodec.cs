using System;

namespace Zbus.Client.Net.Http
{
   public class MessageCodec : ICodec
   {
      public object Decode(IoBuffer buf)
      {
         return Message.Decode(buf);
      }

      public IoBuffer Encode(object obj)
      {
         if(!(obj is Message))
         {
            throw new ArgumentException("Message type required for: " + obj);
         }
         Message msg = obj as Message;
         IoBuffer buf = new IoBuffer();
         msg.Encode(buf);
         buf.Flip();

         return buf;
      }
   }
}
