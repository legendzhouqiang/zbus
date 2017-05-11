namespace Zbus.Client.Net
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

   /// <summary>
   /// The abstraction of remote(may be local as well) invocation
   /// </summary>
   /// <typeparam name="REQ">Request Type</typeparam>
   /// <typeparam name="RES">Response Type</typeparam>
   public interface IInvoker<REQ, RES> 
   {  
      /// <summary>
      /// Send the request and get the response
      /// It can be either Remote Procedure Call or local invocation
      /// </summary>
      /// <param name="req">request message</param>
      /// <param name="timeout">timeout in milliseconds to wait response</param>
      /// <returns>response message</returns>
      RES Invoke(REQ req, int timeout = 10000);
   }
    
}
