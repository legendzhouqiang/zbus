using System;
using System.Collections.Generic;
using System.Text;
using System.Threading.Tasks; 
using Zbus.Mq; 

namespace Zbus.Rpc
{
    public class RpcConfig : MqConfig
    {
        public static readonly string DefaultEncoding = "UTF-8";

        public string ServiceId
        {
            get { return Mq; }
            set { Mq = value; }
        }

        public string Module { get; set; } = "";
        public int Timeout { get; set; } = 10000;
        public string Encoding { get; set; } = DefaultEncoding;
    }

    public class RpcClient
    {
        public IBroker Broker { get; set; }
        public string ServiceId { get; set; } //Equviliant to MQ
        public string Module { get; set; } = "";

        public string AppId { get; set; } = "";
        public string Token { get; set; } = "";
        public string Encoding { get; set; } = "UTF-8";
        public int Timeout { get; set; } = 10000;


        private Encoding encoding = System.Text.Encoding.UTF8;

        public RpcClient(IBroker broker)
        {
            this.Broker = broker;
        }

        public RpcClient(IBroker broker, String mq)
        {
            this.Broker = broker;
            this.ServiceId = mq;
        }

        public RpcClient(RpcConfig config)
        {
            this.Broker = config.Broker;
            this.ServiceId = config.ServiceId;
            this.AppId = config.AppId;
            this.Token = config.Token;
            this.Module = config.Module;
            this.Encoding = config.Encoding;
            this.Timeout = config.Timeout;
        }

        private Message Invoke(Message msg, int timeout = 10000)
        {
            if (msg.Mq == null)
            {
                if (this.ServiceId == null)
                {
                    throw new RpcException("Rpc invocation missing mq");
                }
                else
                {
                    msg.Mq = this.ServiceId;
                }
            }
            msg.Cmd = Protocol.Produce;
            msg.Ack = false;

            return this.Broker.Invoke(msg, timeout);
        }



        public Response Invoke(Request request, int timeout = 10000)
        {
            Message msgReq = new Message();
            if (request.ServiceId != null)
            {
                msgReq.Mq = request.ServiceId;
            }
            msgReq.SetJsonBody(ConvertKit.SerializeObject(request), encoding);

            Message msgRes = Invoke(msgReq, timeout);
            System.Text.Encoding resEncoding = encoding; //default to request encoding
            string encodingName = msgRes.Encoding;
            if (encodingName != null && !encodingName.Equals(Encoding, StringComparison.OrdinalIgnoreCase))
            {
                resEncoding = System.Text.Encoding.GetEncoding(encodingName);
            }

            string jsonString = msgRes.GetBody(resEncoding);

            Response resp = null;
            try
            {
                resp = ConvertKit.DeserializeObject<Response>(jsonString);
            }
            catch
            {
                resp = new Response();
                Dictionary<string, object> jsonRes = ConvertKit.DeserializeObject<Dictionary<string, object>>(jsonString);

                if (jsonRes.ContainsKey("stackTrace") && jsonRes["stackTrace"] != null)
                {
                    resp.StackTrace = (string)jsonRes["stackTrace"];
                    resp.Error = new RpcException(resp.StackTrace);
                }
                else
                {
                    if (jsonRes.ContainsKey("error") && jsonRes["error"] != null)
                    {
                        resp.StackTrace = (string)jsonRes["error"];
                        resp.Error = new RpcException(resp.StackTrace);
                    }
                }

                if (jsonRes.ContainsKey("result"))
                {
                    resp.Result = jsonRes["result"];
                }
            }
            return resp;
        }

        public object Invoke(Type type, string method, params object[] args)
        {
            Request req = new Request
            {
                Module = this.Module,
                Method = method,
                Params = args
            };

            Response resp = Invoke(req, Timeout);
            if (resp.Error != null)
            {
                throw resp.Error;
            }
            return ConvertKit.Convert(resp.Result, type);
        }

        public void Invoke(string method, params object[] args)
        {
            Invoke(typeof(void), method, args);
        }

        public T Invoke<T>(string method, params object[] args)
        {
            return (T)Invoke(typeof(T), method, args);
        }
    }

    public class RpcFactory
    {
        public static T Create<T>(RpcConfig rpcConfig)
        {
            return new MqRpcProxy<T>(rpcConfig).Create();
        }
    }

    class MqRpcProxy<T> : RpcProxy<T>
    {
        private RpcClient rpc;
        public MqRpcProxy(RpcConfig rpcConfig)
        {
            this.rpc = new RpcClient(rpcConfig);
            this.Module = rpcConfig.Module;
        }

        protected override Response Invoke(Type returnType, Request request)
        {
            Response res = rpc.Invoke(request).Result;

            if (res.Result == null) return res;
            if (returnType == typeof(void)) return res;

            if (returnType != res.Result.GetType() && !typeof(Task).IsAssignableFrom(returnType))
            {
                res.Result = ConvertKit.Convert(res.Result, returnType);
            }
            return res;
        }
    }


    public static class RpcProcessorExt
    {
        public static async void OnConsumerMessage(this RpcProcessor processor, Message msg, Consumer consumer)
        {
            Message msgRes = new Message
            {
                Status = "200",
                Recver = msg.Sender,
                Id = msg.Id
            };

            Response response = null;
            try
            {
                string encodingName = msg.Encoding;
                Encoding encoding = processor.Encoding;
                if (encodingName != null)
                {
                    encoding = Encoding.GetEncoding(encodingName);
                }

                Request request = ConvertKit.DeserializeObject<Request>(msg.GetBody(encoding));
                response = await processor.Process(request);
            }
            catch (Exception e)
            {
                response = new Response
                {
                    Error = e
                };
            }

            if (response.Error != null)
            {
                response.StackTrace = "" + response.Error;
                if (response.Error is RpcException)
                {
                    msgRes.Status = "" + ((RpcException)response.Error).Status;
                }
                else
                {
                    msgRes.Status = "500";
                }
            }

            msgRes.SetJsonBody(ConvertKit.SerializeObject(response), processor.Encoding);

            consumer.Route(msgRes);
        }
    }
}
