using System.Threading.Tasks;

namespace Zbus.Net
{

   public interface IId
   {
      string Id { get; set; }
   }

   /// <summary>
   /// The abstraction of remote invocation:
   /// invoke synchronously
   /// invoke asynchronously with a callback
   /// </summary>
   /// <typeparam name="REQ"></typeparam>
   /// <typeparam name="RES"></typeparam>
   public interface IInvoker<REQ, RES> 
   {
      RES InvokeSync(REQ req, int timeout);

      RES InvokeSync(REQ req);

      Task<RES> InvokeAsync(REQ req);
   }
}
