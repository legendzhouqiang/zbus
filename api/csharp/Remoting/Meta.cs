using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace zbus.Remoting
{
    class Meta
    {
        static readonly HashSet<string> httpMethod = new HashSet<string>();
        static IDictionary<string, string> httpStatus = new Dictionary<string, string>();
        static Meta()
        {
            httpMethod.Add("GET");
            httpMethod.Add("POST");
            httpMethod.Add("HEAD");
            httpMethod.Add("PUT");
            httpMethod.Add("DELETE");
            httpMethod.Add("OPTIONS");

            httpStatus.Add("200", "OK");
            httpStatus.Add("201", "Created");
            httpStatus.Add("202", "Accepted");
            httpStatus.Add("204", "No Content");
            httpStatus.Add("206", "Partial Content");
            httpStatus.Add("301", "Moved Permanently");
            httpStatus.Add("304", "Not Modified");
            httpStatus.Add("400", "Bad Request");
            httpStatus.Add("401", "Unauthorized");
            httpStatus.Add("403", "Forbidden");
            httpStatus.Add("404", "Not Found");
            httpStatus.Add("405", "Method Not Allowed");
            httpStatus.Add("416", "Requested Range Not Satisfiable");
            httpStatus.Add("500", "Internal Server Error");
        }

        public string Method { get; set; }
        public string Status { get; set; }
        public string Command { get; set; }
        public IDictionary<string, string> Params;

        public string GetParam(string key)
        {
            return GetParam(key, null);
        }

        public string GetParam(string key, string defaultValue)
        {
            if (this.Params == null)
                return defaultValue;
            string value;
            this.Params.TryGetValue(key, out value);
            if (value == null)
                value = defaultValue;
            return value;
        }

        public void SetParam(string key, string val)
        {
            if (this.Params == null)
            {
                this.Params = new Dictionary<string, string>();
            }
            this.Params[key] = val;
        }



        public Meta(string meta)
        {
            this.Method = "GET";
            if (meta == null || meta.Trim().Equals("")) return;
            string[] blocks = meta.Split(' ');
            string method = blocks[0];
            if (!httpMethod.Contains(method))
            {
                this.Status = blocks[1];
                return;
            }
            this.Method = method;
            this.DecodeCommand(blocks[1]);
        }

        public override string ToString()
        {
            if (this.Status != null)
            {
                string desc = null;
                httpStatus.TryGetValue(this.Status, out desc);
                if (desc == null)
                {
                    desc = "Unknown Status";
                }
                return string.Format("HTTP/1.1 {0} {1}", this.Status, desc);
            }
            if (this.Command != null)
            {
                string cmd = this.EncodeCommand();
                return string.Format("{0} /{1} HTTP/1.1", this.Method, cmd);
            }
            return "";
        }


        public string EncodeCommand()
        {
            StringBuilder sb = new StringBuilder();
            if (this.Command != null)
            {
                sb.Append(this.Command);
            }
            if (this.Params == null)
            {
                return sb.ToString();
            }
            if (this.Params.Count > 0)
            {
                sb.Append("?");
            }
            foreach (KeyValuePair<string, string> kvp in this.Params)
            {
                sb.Append(string.Format("{0}={1}&", kvp.Key, kvp.Value));
            }
            return sb.ToString(0, sb.Length - 1);
        }


        public void DecodeCommand(string cmd)
        {
            int idx = cmd.IndexOf('?');
            if (idx < 0)
            {
                this.Command = cmd;
            }
            else
            {
                this.Command = cmd.Substring(0, idx);
            }
            if (this.Command[0] == '/')
            {
                this.Command = this.Command.Substring(1);
            }
            if (idx < 0) return;
            string args = cmd.Substring(idx + 1);
            this.Params = new Dictionary<string, string>();
            string[] blocks = args.Split('&');
            foreach (string kv in blocks)
            {
                string[] kvb = kv.Split("=".ToCharArray(), 2);
                this.Params[kvb[0].Trim()] = kvb[1].Trim();
            }
        }

    }
}
