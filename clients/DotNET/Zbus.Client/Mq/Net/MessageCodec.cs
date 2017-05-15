using System;
using Zbus.Client.Net;

namespace Zbus.Client.Mq.Net
{
   public class MessageCodec : ICodec
   {
      public object Decode(ByteBuffer buf)
      {
         return Message.Decode(buf);
      }

      public ByteBuffer Encode(object obj)
      {
         if(!(obj is Message))
         {
            throw new ArgumentException("Message type required for: " + obj);
         }
         Message msg = obj as Message;
         ByteBuffer buf = new ByteBuffer();
         msg.Encode(buf);
         buf.Flip();

         return buf;
      }
   }
}
