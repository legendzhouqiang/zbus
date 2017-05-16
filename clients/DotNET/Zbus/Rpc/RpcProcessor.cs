using System;
using System.Collections.Generic;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zbus.Mq;
namespace Zbus.Rpc
{

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
                if (module == "")
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
}