using System;
using System.Collections.Generic;
using System.Dynamic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Rpc
{
    public class RpcInvoker : DynamicObject
    {
        public Broker Broker { get; set; }  

        public string Topic { get; set; }
        public string Module { get; set; }
        public string Token { get; set; }
        public Encoding Encoding { get; set; }
        public ServerSelector RpcServerSelector { get; set; } 

        private Producer producer;
         

        public RpcInvoker(Broker broker, string topic = null)
        { 
            this.producer = new Producer(broker);
            Topic = topic; 
        } 
        private async Task<Message> InvokeAsync(Message msg, CancellationToken? token = null, ServerSelector selector = null)
        {
            if (msg.Topic == null)
            {
                msg.Topic = this.Topic; 
            }
            if (msg.Topic == null)
            {
                throw new RpcException("Message missing topic");
            }  

            msg.Cmd = Protocol.PRODUCE;
            msg.Ack = false;
            msg.Token = Token; 
            return await producer.ProduceAsync(msg, token, selector); 
        }  

        private ServerSelector GetSelector(ServerSelector selector = null)
        {
            if (selector == null) return RpcServerSelector;
            return selector;
        }

        public async Task<Response> InvokeAsync(Request request, string topic=null, CancellationToken? token = null, ServerSelector selector = null)
        {
            Message msgReq = new Message();
            if (topic == null) topic = this.Topic; 

            msgReq.SetJsonBody(JsonKit.SerializeObject(request), Encoding); 
            Message msgRes = await InvokeAsync(msgReq, token, GetSelector(selector));

            string bodyString = msgRes.BodyString;
            Response resp = null;
            try
            {
                resp = JsonKit.DeserializeObject<Response>(bodyString);
            }
            catch
            {
                resp = new Response(); 
                resp.StackTrace = bodyString; 
            }
            return resp;
        }

        public async Task<object> InvokeAsync(Type type, string method, params object[] args)
        {
            Request req = new Request
            {
                Module = this.Module,
                Method = method,
                Params = args
            };

            Response resp = await InvokeAsync(req, null, null, RpcServerSelector); 
            if (resp.Error != null) 
            {
                throw resp.Error;
            }
            if(resp.StackTrace != null)
            {
                throw new MqException(resp.StackTrace);
            }
            return JsonKit.Convert(resp.Result, type);
        }

        public async Task InvokeAsync(string method, params object[] args)
        {
            await InvokeAsync(typeof(void), method, args);
        }

        public async Task<T> InvokeAsync<T>(string method, params object[] args)
        {
            return (T)await InvokeAsync(typeof(T), method, args);
        }



        public override bool TryInvokeMember(InvokeMemberBinder binder, object[] args, out object result)
        {
            string method = binder.Name;
            Request req = new Request
            {
                Method = method,
                Params = args,
            };

            try
            {
                Response resp = this.InvokeAsync(req).Result;
                if (resp.StackTrace != null)
                {
                    if (resp.Error != null)
                    {
                        throw resp.Error;
                    }
                    throw new RpcException(resp.StackTrace);
                }
                result = JsonKit.Convert(resp.Result, binder.ReturnType);
            }
            catch (Exception e)
            {
                throw new RpcException(e.Message, e);
            }

            return true;
        }

        public T CreateProxy<T>()
        {
            return new RpcProxy<T>(this).Create();
        }
    } 

}
