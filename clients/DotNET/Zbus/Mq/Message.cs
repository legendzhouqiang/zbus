using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;
using Zbus.Net; 

namespace Zbus.Mq
{
    public class Message : Id
    {
        private string url = "/";
        private string status;
        private string method = "GET";

        private IDictionary<string, string> head = new Dictionary<string, string>();
        private byte[] body;

        public string GetHead(string key)
        {
            return GetHead(key, null);
        }

        public string GetHead(string key, string defaultValue)
        {
            string val = null;
            this.head.TryGetValue(key, out val);
            if (val == null)
            {
                val = defaultValue;
            }
            return val;
        }

        public void SetHead(string key, string val)
        {
            this.head[key] = val;
        }

        public void RemoveHead(string key)
        {
            this.head.Remove(key);
        }

        public void SetBody(byte[] body)
        {
            this.body = body;
            int bodyLen = 0;
            if (this.body != null)
            {
                bodyLen = this.body.Length;
            }
            this.SetHead("content-length", string.Format("{0}", bodyLen));
        }

        public void SetJsonBody(string body, System.Text.Encoding encoding)
        {
            this.SetBody(body, encoding);
            this.SetHead("content-type", "application/json");
        }

        public void SetJsonBody(string body)
        {
            SetJsonBody(body, System.Text.Encoding.Default);
        }

        public void SetBody(string body, System.Text.Encoding encoding)
        {
            Encoding = encoding.WebName;
            SetBody(encoding.GetBytes(body));
        }

        public void SetBody(string body)
        {
            SetBody(body, System.Text.Encoding.Default);
        }

        public void SetBody(string format, params object[] args)
        {
            SetBody(string.Format(format, args));
        }

        #region AUX_GET_SET

        public String Topic
        {
            get { return GetHead(Protocol.TOPIC); }
            set { SetHead(Protocol.TOPIC, value); }
        }

        public String Cmd
        {
            get { return GetHead(Protocol.COMMAND); }
            set { SetHead(Protocol.COMMAND, value); }
        }

        public String Id
        {
            get { return GetHead(Protocol.ID); }
            set { SetHead(Protocol.ID, value); }
        } 

        public String Token
        {
            get { return GetHead(Protocol.TOKEN); }
            set { SetHead(Protocol.TOKEN, value); }
        }

        public String Sender
        {
            get { return GetHead(Protocol.SENDER); }
            set { SetHead(Protocol.SENDER, value); }
        }

        public String Recver
        {
            get { return GetHead(Protocol.RECVER); }
            set { SetHead(Protocol.RECVER, value); }
        } 
        public String Encoding
        {
            get { return GetHead(Protocol.ENCODING); }
            set { SetHead(Protocol.ENCODING, value); }
        }

        public String OriginUrl
        {
            get { return GetHead(Protocol.ORIGIN_URL); }
            set { SetHead(Protocol.ORIGIN_URL, value); }
        }

        public String OriginStatus
        {
            get { return GetHead(Protocol.ORIGIN_STATUS); }
            set { SetHead(Protocol.ORIGIN_STATUS, value); }
        }

        public String OriginId
        {
            get { return GetHead(Protocol.ORIGIN_ID); }
            set { SetHead(Protocol.ORIGIN_ID, value); }
        }

        public bool Ack
        {
            get
            {
                string ack = GetHead(Protocol.ACK);
                return (ack == null) || "1".Equals(ack); //default to true 
            }
            set { SetHead(Protocol.ACK, value ? "1" : "0"); }
        } 

        public string Url
        {
            get { return this.url; }
            set { this.url = value; }
        }

        public string Status
        {
            get { return this.status; }
            set { this.status = value; }
        }

        public string Method
        {
            get { return this.method; }
            set { this.method = value; }
        }


        public byte[] Body
        {
            get { return body; }
            set { SetBody(value); }
        }

        public string BodyString
        {
            get { return GetBody(); }
            set { SetBody(value); }
        }

        public string BodyJson
        {
            get { return GetBody(); }
            set { SetJsonBody(value); }
        }


        #endregion

        public void Encode(ByteBuffer buf)
        {
            if(this.status != null)
            {
                string desc = "Unknow status";
                if (HttpStatusTable.ContainsKey(this.status))
                {
                    desc = HttpStatusTable[this.status];
                }
                buf.Put("HTTP/1.1 {0} {1}\r\n", this.status, desc);
            }
            else
            {
                string method = this.method;
                if(method == null)
                {
                    method = "GET";
                }
                string url = this.url;
                if(url == null)
                {
                    url = "/";
                }
                buf.Put("{0} {1} HTTP/1.1\r\n", method, url);
            } 
            foreach (KeyValuePair<string, string> e in this.head)
            {
                buf.Put("{0}: {1}\r\n", e.Key, e.Value);
            }
            string lenKey = "content-length";
            if (!this.head.ContainsKey(lenKey))
            {
                int bodyLen = this.body == null ? 0 : this.body.Length;
                buf.Put("{0}: {1}\r\n", lenKey, bodyLen);
            }

            buf.Put("\r\n");

            if (this.body != null)
            {
                buf.Put(this.body);
            }
        }

        private static int FindHeaderEnd(ByteBuffer buf)
        {
            int i = buf.Position;
            byte[] data = buf.Data;
            while (i + 3 < buf.Limit)
            {
                if (data[i] == '\r' && data[i + 1] == '\n' && data[i + 2] == '\r' && data[i + 3] == '\n')
                {
                    return i + 3;
                }
                i++;
            }
            return -1;

        }

        public static Message DecodeHeader(string header)
        {
            Message msg = new Message();
            string[] lines = Regex.Split(header, "\r\n");
            string meta = lines[0].Trim();
            string[] blocks = meta.Split(' ');
            string test = blocks[0].ToUpper();
            if (test.StartsWith("HTTP"))
            {
                msg.status = blocks[1]; 
            }
            else
            {
                msg.method = blocks[0];
                if (blocks.Length > 1)
                {
                    msg.url = blocks[1];
                }
            } 

            for (int i = 1; i < lines.Length; i++)
            {
                string line = lines[i];
                int idx = line.IndexOf(':');
                if (idx < 0) continue; //ignore
                string key = line.Substring(0, idx).Trim().ToLower(); //key to lower case
                string val = line.Substring(idx + 1).Trim();
                msg.SetHead(key, val);
            }

            return msg;
        }

        public static Message Decode(ByteBuffer buf)
        {
            int idx = FindHeaderEnd(buf);
            int headLen = idx - buf.Position + 1;
            if (idx < 0) return null;

            string header = System.Text.Encoding.Default.GetString(buf.Data, buf.Position, headLen);

            Message msg = DecodeHeader(header);
            string bodyLenString = msg.GetHead("content-length");
            if (bodyLenString == null)
            {
                buf.Drain(headLen);
                return msg;
            }
            int bodyLen = int.Parse(bodyLenString);
            if (buf.Remaining() < headLen + bodyLen)
            {
                return null;
            }
            buf.Drain(headLen);
            byte[] body = buf.Get(bodyLen);
            msg.SetBody(body);
            return msg;
        }

        public override string ToString()
        {
            ByteBuffer buf = new ByteBuffer();
            Encode(buf);
            return buf.ToString();
        }

        public string GetBody()
        {
            if (body == null) return null;
            return System.Text.Encoding.Default.GetString(body);
        }

        public string GetBody(System.Text.Encoding encoding)
        {
            if (body == null) return null;
            return encoding.GetString(body);
        }

        private static IDictionary<string, string> HttpStatusTable = new Dictionary<string, string>();
        static Message()
        {
            HttpStatusTable.Add("200", "OK");
            HttpStatusTable.Add("201", "Created");
            HttpStatusTable.Add("202", "Accepted");
            HttpStatusTable.Add("204", "No Content");
            HttpStatusTable.Add("206", "Partial Content");
            HttpStatusTable.Add("301", "Moved Permanently");
            HttpStatusTable.Add("304", "Not Modified");
            HttpStatusTable.Add("400", "Bad Request");
            HttpStatusTable.Add("401", "Unauthorized");
            HttpStatusTable.Add("403", "Forbidden");
            HttpStatusTable.Add("404", "Not Found");
            HttpStatusTable.Add("405", "Method Not Allowed");
            HttpStatusTable.Add("416", "Requested Range Not Satisfiable");
            HttpStatusTable.Add("500", "Internal Server Error");
        }

    } 
}
