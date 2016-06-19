using System;

namespace Zbus.Mq
{
    public class MqException : Exception
    {
        public MqException()
        {
        }

        public MqException(string message)
            : base(message)
        {
        }
        public MqException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}
