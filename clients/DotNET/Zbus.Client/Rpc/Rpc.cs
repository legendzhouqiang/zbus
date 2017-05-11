using Newtonsoft.Json;
using Newtonsoft.Json.Serialization;
using System;
using System.Collections.Generic;
using System.Linq.Expressions;
using System.Net.Http;
using System.Reflection;
using System.Runtime.Remoting.Messaging;
using System.Runtime.Remoting.Proxies;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;

namespace Zbus.Client.Rpc
{
   /// <summary>
   /// Method+Params stands for a Rpc request.
   /// 
   /// To support hierachy
   /// ServiceId:Module:Method if the full reference of a method.
   /// By default both ServiceId and Module default to null. 
   /// 
   /// </summary>
   public class Request
   {
      public string Method { get; set; }
      public object[] Params { get; set; }

      public string ServiceId { get; set; } //optional
      public string Module { get; set; }//optional 
   }

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


   public class Remote : Attribute
   {
      public string Id { get; set; }
      public bool Exclude { get; set; }

      public Remote()
      {
         Id = null;
      }

      public Remote(string id)
      {
         this.Id = id;
      }

      public Remote(bool exclude)
      {
         this.Exclude = exclude;
      }
   }



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

   public class RpcProcessor
   {
      public Encoding Encoding { get; set; } = Encoding.UTF8;

      private Dictionary<string, MethodInstance> methods = new Dictionary<string, MethodInstance>();

      public void AddModule<T>(string module = null)
      {
         Type type = typeof(T);
         object instance = Activator.CreateInstance(type);
         if (module == null)
         {
            AddModule(instance);
         }
         else
         {
            AddModule(module, instance);
         }
      }

      public void AddModule(object service)
      {
         IDictionary<string, MethodInstance> table = BuildMethodTable(service); 
         foreach (Type type in service.GetType().GetInterfaces())
         {
            AddModule(table, type.Name, service);
            AddModule(table, type.FullName, service);
         }

         AddModule(table, "", service);
         AddModule(table, service.GetType().Name, service);
         AddModule(table, service.GetType().FullName, service);
      }

      public void AddModule(string module, object service)
      {
         IDictionary<string, MethodInstance> table = BuildMethodTable(service);
         AddModule(table, module, service);
      }

      private void AddModule(IDictionary<string, MethodInstance> table, string module, object service)
      { 
         foreach (var kv in table)
         {
            string id = module + ":" + kv.Key;
            this.methods[id] = kv.Value;
         }
      }

      private IDictionary<string, MethodInstance> BuildMethodTable(object service)
      {
         IDictionary<string, MethodInstance> table = new Dictionary<string, MethodInstance>();
         IDictionary<string, MethodInstance> ignore = new Dictionary<string, MethodInstance>();
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
               if (info.DeclaringType != type || !info.IsPublic) continue;

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

               string paramMD5 = "";
               foreach (ParameterInfo pInfo in info.GetParameters())
               {
                  paramMD5 += pInfo.ParameterType;
               }
               string key = id + ":" + paramMD5;
               string key2 = id; 

               MethodInstance instance = new MethodInstance(info, service);
               if (exclude)
               {
                  ignore[key] = instance;
                  ignore[key2] = instance;
               }
               else
               {
                  table[key] = instance;
                  table[key2] = instance;
               } 
            } 
         }
         foreach (string key in ignore.Keys)
         {
            table.Remove(key);
         }
         return table;
      }

      private MethodInstance FindMethod(string module, string method, object[] args)
      {
         string paramMD5 = null;
         foreach (object arg in args)
         {
            paramMD5 += arg.GetType();
         }
         MethodInstance m = FindMethod(module, method, paramMD5);
         if (m != null) return m;
         m = FindMethod(module, method, (string)null); 
         return m;
      }
      private MethodInstance FindMethod(string module, string method, string paramMD5)
      {
         MethodInstance m = FindMethod0(module, method, paramMD5);
         if (m != null) return m;
         string camelMethod = char.ToUpper(method[0]) + method.Substring(1);
         if (!method.Equals(camelMethod))
         {
            m = FindMethod0(module, camelMethod, paramMD5);
         }
         return m;
      }
      private MethodInstance FindMethod0(string module, string method, string paramMD5)
      {
         string async = "Async";

         string key = module + ":" + method;
         if (paramMD5 != null)
         {
            key += ":" + paramMD5;
         }
         if (this.methods.ContainsKey(key))
         {
            return this.methods[key];
         } 

         if (method.EndsWith(async)) //special for Async method
         {
            key = module + ":" + method.Substring(0, method.Length - async.Length);
            if (paramMD5 != null)
            {
               key += ":" + paramMD5;
            }

            if (this.methods.ContainsKey(key))
            {
               return this.methods[key];
            }
         }
         return null;
      }

      public async Task<Response> Process(Request request)
      {
         Response response = new Response();
         string module = request.Module == null ? "" : request.Module;
         string method = request.Method;
         object[] args = request.Params;

         MethodInstance target = null;
         if (request.Method == null)
         {
            response.Error = new RpcException("missing method name", 400);
            return response;
         }

         target = FindMethod(module, method, args);
         if (target == null)
         {
            string errorMsg = module + "." + method + " Not Found";
            if(module == "")
            {
               errorMsg = method + " Not Found";
            }
            response.Error = new RpcException(errorMsg, 404);
            return response;
         }

         try
         {
            ParameterInfo[] pinfo = target.Method.GetParameters();
            if (pinfo.Length != args.Length)
            { 
               response.Error = new RpcException("number of argument not match", 400);
               return response;
            } 

            for (int i = 0; i < pinfo.Length; i++)
            {
               if (args[i].GetType() != pinfo[i].ParameterType)
               {
                  args[i] = ConvertKit.Convert(args[i], pinfo[i].ParameterType);
               }
            }

            dynamic invoked = target.Method.Invoke(target.Instance, args);
            if (invoked != null && typeof(Task).IsAssignableFrom(invoked.GetType()))
            {
               if (target.Method.ReturnType.GenericTypeArguments.Length > 0)
               {
                  response.Result = await invoked;
               }
               else
               {
                  response.Result = null;
               }
            }
            else
            {
               response.Result = invoked;
            }
            return response; 
         }
         catch (Exception ex)
         {
            response.Error = ex;
            if (ex.InnerException != null)
            {
               response.Error = ex.InnerException;
            }
            return response;
         }
      } 

      private class MethodInstance
      {
         public MethodInfo Method { get; set; }
         public object Instance { get; set; }

         public MethodInstance(MethodInfo method, object instance)
         {
            this.Method = method;
            this.Instance = instance;
         }
      }
   }

   public abstract class RpcProxy<T> : RealProxy
   {
      public string Module { get; set; } = "";
      public string Encoding { get; set; } = "UTF-8";

      public RpcProxy() : base(typeof(T))
      {

      }

      public T Create()
      {
         return (T)GetTransparentProxy();
      }
       
      protected abstract Response Invoke(Type realReturnType, Request request);

      public dynamic Request(Type realReturnType, Request request)
      {
         Response res = Invoke(realReturnType, request);

         if (res.Error != null)
         {
            throw res.Error;
         }
         return res.Result;
      }

      public override IMessage Invoke(IMessage msg)
      {
         var methodCall = (IMethodCallMessage)msg;
         var method = (MethodInfo)methodCall.MethodBase;
         if (method.DeclaringType.FullName.Equals("System.IDisposable"))
         {
            return new ReturnMessage(null, null, 0, methodCall.LogicalCallContext, methodCall);
         }
         if (method.DeclaringType.Name.Equals("Object"))
         {
            var result = method.Invoke(this, methodCall.Args);
            return new ReturnMessage(result, null, 0, methodCall.LogicalCallContext, methodCall);
         }

         try
         {
            string methodName = methodCall.MethodName;
            object[] args = methodCall.Args;

            Request req = new Request
            {
               Method = methodName,
               Params = args,
               Module = this.Module, 
            };

            Type returnType = method.ReturnType;

            //Simple methods
            if (!typeof(Task).IsAssignableFrom(returnType))
            {
               Response res = Invoke(returnType, req);

               if (res.Error != null)
               {
                  return new ReturnMessage(res.Error, methodCall);
               }
               return new ReturnMessage(res.Result, null, 0, methodCall.LogicalCallContext, methodCall);
            }

            //Task returned method 
            Type realType = typeof(void);
            if (returnType.GenericTypeArguments.Length >= 1)
            {
               realType = returnType.GenericTypeArguments[0];
            }

            Task task = null;
            if (realType == typeof(void))
            {
               task = Task.Run(() =>
               {
                  Invoke(realType, req);
               });
            }
            else
            {
               MethodInfo invokeMethod = this.GetType().GetRuntimeMethod("Request", new Type[] { typeof(Type), typeof(Request) });

               var calledExp = Expression.Call(
                  Expression.Constant(this),
                  invokeMethod,
                  Expression.Constant(realType),
                  Expression.Constant(req)
               );

               var castedExp = Expression.Convert(calledExp, realType);

               var d = Expression.Lambda(castedExp).Compile();
               task = (Task)Activator.CreateInstance(returnType, d);
               task.Start();
            }

            return new ReturnMessage(task, null, 0, methodCall.LogicalCallContext, methodCall);

         }
         catch (Exception e)
         {
            if (e is TargetInvocationException && e.InnerException != null)
            {
               return new ReturnMessage(e.InnerException, msg as IMethodCallMessage);
            }
            return new ReturnMessage(e, msg as IMethodCallMessage);
         }
      }
   }

   public static class ConvertKit
   {
      public static JsonSerializerSettings JsonSettings = new JsonSerializerSettings
      {
         ContractResolver = new CamelCasePropertyNamesContractResolver(),
         TypeNameHandling = TypeNameHandling.Objects,
      };

      public static string SerializeObject(object value)
      {
         return JsonConvert.SerializeObject(value, JsonSettings);
      }

      public static T DeserializeObject<T>(string value)
      {
         return JsonConvert.DeserializeObject<T>(value, JsonSettings);
      } 

      public static object Convert(object raw, Type type)
      {
         if(raw == null)
         {
            return null;
         }

         if (type == typeof(void)) return null;

         if (raw.GetType().IsAssignableFrom(type)) return raw;

         string jsonRaw = JsonConvert.SerializeObject(raw);
         return JsonConvert.DeserializeObject(jsonRaw, type, JsonSettings);
      }  
   }
}