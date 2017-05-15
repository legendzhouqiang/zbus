using System;

namespace Zbus.Client.Rpc
{
    /// <summary>
    /// Result or Error
    /// Rule to judge: 
    /// 1) if StackTrace prompt, it is an Error response, the Error may be deserialized, 
    /// 2) else it is a normal result  
    /// 
    /// </summary>
    public class Response
    {
        public dynamic Result { get; set; }
        /// <summary>
        /// With value indicates Error returned, otherwise No error, check Result, it is a json value(empty included)
        /// </summary>
        public string StackTrace { get; set; }
        public Exception Error { get; set; }
    }
}