namespace Zbus.Net
{
    /// <summary>
    /// Identity interface to track message match for asynchroneous invocation.
    /// </summary>
    public interface IId
    {
        /// <summary>
        /// Identity string
        /// </summary>
        string Id { get; set; }
    }
    public interface IClient<REQ, RES> : IPoolable
    {
        void Send(REQ req, int timeout = 3000);
        RES Recv(int timeout = 10000);
       
        /// <summary>
        /// Send the request and get the response
        /// It can be either Remote Procedure Call or local invocation
        /// </summary>
        /// <param name="req">request message</param>
        /// <param name="timeout">timeout in milliseconds to wait response</param>
        /// <returns>response message</returns>
        RES Invoke(REQ req, int timeout = 3000);

        V Attr<V>(string key);
        void Attr<V>(string key, V value);

        void StartHeartbeat(int heartbeatInterval);
        void StopHeartbeat();
        void Heartbeat();
    }
}
