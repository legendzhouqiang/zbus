using System;


namespace Zbus.RPC
{
    public class RpcException : Exception
    {
        public RpcException()
        {
        }

        public RpcException(string message)
            : base(message)
        {
        }
        public RpcException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }
}
