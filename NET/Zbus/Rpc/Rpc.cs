using System;
using System.Collections.Generic;
using System.Text;

using Zbus.Kit.Json;
using Zbus.Mq;
using Zbus.Broker;
using Zbus.Net.Http;

namespace Zbus.RPC
{
   public class Request
   {
      private string mq; //
      private string module;
      private string method;
      private object[] args;
      private object[] argTypes;
      private string encoding = "UTF-8";


      public string Mq
      {
         get { return mq; }
         set { mq = value; }
      }

      public string Module
      {
         get { return module; }
         set { module = value; }
      }
      public string Method
      {
         get { return method; }
         set { method = value; }
      }

      public object[] Args
      {
         get { return args; }
         set { args = value; }
      }

      public object[] ArgTypes
      {
         get { return argTypes; }
         set { argTypes = value; }
      }

      public string Encoding
      {
         get { return encoding; }
         set { encoding = value; }
      }
   }

   public class Rpc
   {
      private IBroker broker;
      private string mq;
      private string token = "";
      private String module = "";
      private String encoding = RpcConfig.DEFAULT_ENCODING;
      private int timeout = 10000;

      public Rpc(IBroker broker)
      {
         this.broker = broker;
      }

      public Rpc(IBroker broker, String mq)
      {
         this.broker = broker;
         this.mq = mq;
      }

      public Rpc(RpcConfig config)
      {
         this.broker = config.Broker;
         this.mq = config.Mq;
         this.token = config.AccessToken;
         this.module = config.Module;
         this.encoding = config.Encoding;
         this.timeout = config.Timeout;
      }

      private Message Invoke(Message msg, int timeout)
      {
         if (msg.Mq == null)
         {
            if (this.mq == null)
            {
               throw new RpcException("Rpc invocation missing mq");
            }
            else
            {
               msg.Mq = this.mq;
            }
         }
         msg.Cmd = Proto.Produce;
         msg.Ack = false;

         return this.broker.InvokeSync(msg, timeout);
      }

      public object Invoke(Request request)
      {
         IDictionary<string, object> req = new Dictionary<string, object>();
         req["module"] = request.Module;
         req["method"] = request.Method;
         req["params"] = request.Args;
         req["paramTypes"] = request.ArgTypes;
         req["encoding"] = request.Encoding;

         Message msgReq = new Message();
         string json = JSON.Instance.ToJSON(req);
         msgReq.SetJsonBody(json);
         if (request.Mq != null)
         {
            msgReq.Mq = request.Mq;
         }


         Message msgRes = this.Invoke(msgReq, this.timeout);
         string encodingName = msgRes.Encoding;
         Encoding encoding = Encoding.Default;
         if (encodingName != null)
         {
            encoding = Encoding.GetEncoding(encodingName);
         }
         string jsonString = msgRes.GetBody(encoding);
         Dictionary<string, object> jsonRes = (Dictionary<string, object>)JSON.Instance.Parse(jsonString);
         if (jsonRes.ContainsKey("error"))
         {
            if (jsonRes["error"] != null)
               throw new RpcException((string)jsonRes["error"]);
         }

         if (jsonRes.ContainsKey("result"))
         {
            return jsonRes["result"];
         }


         throw new RpcException("return format error");
      }

      public object Invoke(string method, params object[] args)
      {
         Request req = new Request();
         req.Module = this.module;
         req.Method = method;
         req.Args = args;
         req.Encoding = this.encoding;

         return Invoke(req);
      }
   }

}
