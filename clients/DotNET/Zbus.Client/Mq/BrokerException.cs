using System;
using System.Runtime.Serialization;

namespace Zbus.Client.Broker
{
    public class BrokerException : Exception
    {
        public BrokerException()
        {
        }

        public BrokerException(SerializationInfo info, StreamingContext context) : base(info, context)
        {

        }

        public BrokerException(string message)
            : base(message)
        {
        }
        public BrokerException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}
