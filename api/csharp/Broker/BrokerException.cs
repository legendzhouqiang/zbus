using System;


namespace Zbus.Broker
{
    public class BrokerException : Exception
    {
        public BrokerException()
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
