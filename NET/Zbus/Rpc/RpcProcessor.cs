using System;
using System.Collections.Generic; 
using System.Text;
using System.Reflection;
using System.Collections;
 
using Zbus.Kit.Json;
using Zbus.Net.Http;

namespace Zbus.RPC
{
   public class RpcProcessor : IMessageProcessor
   {

      private Encoding encoding;
      private Dictionary<string, MethodInstance> methods = new Dictionary<string, MethodInstance>();

      public RpcProcessor(params object[] services)
      {
         this.Init(Encoding.UTF8, services);
      }

      public RpcProcessor(Encoding encoding, params object[] services)
      {
         this.Init(encoding, services);
      }

      private void Init(Encoding encoding, params object[] services)
      {
         this.encoding = encoding;
         foreach (object service in services)
         {
            this.AddModule(service);
         }
      }

      public void AddModule(object service)
      {
         foreach (Type type in service.GetType().GetInterfaces())
         {
            AddModule(type.Name, service);
            AddModule(type.FullName, service);
         }

         AddModule("", service);
         AddModule(service.GetType().Name, service);
         AddModule(service.GetType().FullName, service);
      }

      public void AddModule(string module, object service)
      {
         List<Type> types = new List<Type>();
         types.Add(service.GetType());
         foreach (Type type in service.GetType().GetInterfaces())
         {
            types.Add(type);
         }
         foreach (Type type in types)
         {
            foreach (MethodInfo info in type.GetMethods())
            {
               bool exclude = false;
               string id = info.Name;

               foreach (Attribute attr in Attribute.GetCustomAttributes(info))
               {
                  if (attr.GetType() == typeof(Remote))
                  {
                     Remote r = (Remote)attr;
                     if (r.Id != null)
                     {
                        id = r.Id;
                     }
                     if (r.Exclude)
                     {
                        exclude = true;
                     }
                     break;
                  }
               }
               if (exclude) continue;

               string paramMD5 = "";
               foreach (ParameterInfo pInfo in info.GetParameters())
               {
                  paramMD5 += pInfo.ParameterType;
               }
               string key = module + ":" + id + ":" + paramMD5;
               string key2 = module + ":" + id;

               MethodInstance instance = new MethodInstance(info, service);
               this.methods[key] = instance;
               this.methods[key2] = instance;
            }
         }

      }

      public Message Process(Message request)
      {
         string json = request.GetBody();

         System.Exception error = null;
         object result = null;

         string module = "";
         string method = null;
         ArrayList args = null;

         MethodInstance target = null;

         Dictionary<string, object> parsed = null;
         try
         {
            parsed = (Dictionary<string, object>)JSON.Instance.Parse(json);
         }
         catch (System.Exception ex)
         {
            error = ex;
         }
         if (error == null)
         {
            try
            {
               if (parsed.ContainsKey("module"))
               {
                  module = (string)parsed["module"];
               }
               method = (string)parsed["method"];
               args = (ArrayList)parsed["params"];
               if (args == null)
               {
                  args = new ArrayList();
               }
            }
            catch (System.Exception ex)
            {
               error = ex;
            }
            if (method == null)
            {
               error = new RpcException("missing method name");
            }
         }

         if (error == null)
         {
            string paramMD5 = "";
            foreach (object arg in args)
            {
               paramMD5 += arg.GetType();
            }

            string key = module + ":" + method + ":" + paramMD5;
            string key2 = module + ":" + method;
            if (this.methods.ContainsKey(key))
            {
               target = this.methods[key];
            }
            else
            {
               if (this.methods.ContainsKey(key2))
               {
                  target = this.methods[key2];
               }
               else
               {
                  error = new RpcException(module + "." + method + " not found");
               }
            }
         }

         if (error == null)
         {
            try
            {
               ParameterInfo[] pinfo = target.Method.GetParameters();
               if (pinfo.Length == args.Count)
               {
                  object[] paras = new object[args.Count];
                  for (int i = 0; i < pinfo.Length; i++)
                  {
                     paras[i] = System.Convert.ChangeType(args[i], pinfo[i].ParameterType);
                  }
                  result = target.Method.Invoke(target.Instance, paras);
               }
               else
               {
                  error = new RpcException("number of argument not match");
               }
            }
            catch (System.Exception ex)
            {
               error = ex;
            }
         }

         Dictionary<string, object> data = new Dictionary<string, object>();
         if (error == null)
         {
            data["error"] = null;
            data["result"] = result;
         }
         else
         {
            data["error"] = error.Message;
            data["result"] = null;
         }

         string resJson = JSON.Instance.ToJSON(data);
         Message res = new Message();
         res.SetBody(resJson);

         return res;
      }
   }


}
