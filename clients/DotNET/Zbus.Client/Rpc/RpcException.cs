using System;
using System.Runtime.Serialization;

namespace Zbus.Client.Rpc
{

    public class RpcException : Exception
    {
        public int Status { get; set; } = 500;

        public RpcException(SerializationInfo info, StreamingContext context) : base(info, context)
        {

        }

        public RpcException(int status = 500)
        {
            this.Status = status;
        }

        public RpcException(string message, int status = 500)
            : base(message)
        {
            this.Status = status;
        }
        public RpcException(string message, Exception inner, int status = 500)
            : base(message, inner)
        {
            this.Status = status;
        }
    }
}