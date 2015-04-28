using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Text.RegularExpressions;

namespace zbus.Remoting
{
    
    public class Message
    {
        public static readonly string HEADER_CLIENT     = "remote-addr";
        public static readonly string HEADER_ENCODING   = "content-encoding";

        public static readonly string HEADER_CMD        = "cmd";
        public static readonly string HEADER_SUBCMD     = "sub_cmd";
        public static readonly string HEADER_BROKER     = "broker";
        public static readonly string HEADER_TOPIC      = "topic";
        public static readonly string HEADER_MQ_REPLY   = "mq_reply";
        public static readonly string HEADER_MQ         = "mq";
        public static readonly string HEADER_TOKEN      = "token";
        public static readonly string HEADER_MSGID      = "msgid";
        public static readonly string HEADER_MSGID_RAW  = "msgid_raw";
        public static readonly string HEADER_ACK        = "ack";
        public static readonly string HEADER_REPLY_CODE = "reply_code";

        private Meta meta = new Meta("");
        private IDictionary<string, string> head = new Dictionary<string,string>();
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

        public string GetHeadOrParam(string key)
        {
            return GetHeadOrParam(key, null);
        }

        public string GetHeadOrParam(string key, string defaultValue)
        {
            string val = GetHead(key);
            if(val == null){
                val = this.meta.GetParam(key, defaultValue);
            }
            return val;
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

        public void SetBody(string body,  System.Text.Encoding encoding)
        {
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

        public String MqReply
        {
            get
            {
                return GetHeadOrParam(HEADER_MQ_REPLY);
            }
            set
            {
                SetHead(HEADER_MQ_REPLY, value);

            }
        }
        public String Mq
        {
            get
            {
                return GetHeadOrParam(HEADER_MQ);
            }
            set
            {
                SetHead(HEADER_MQ, value);

            }
        }

        public String Command
        {
            get
            {
                return GetHeadOrParam(HEADER_CMD);
            }
            set
            {
                SetHead(HEADER_CMD, value);

            }
        }

        public String SubCommand
        {
            get
            {
                return GetHeadOrParam(HEADER_SUBCMD);
            }
            set
            {
                SetHead(HEADER_SUBCMD, value);

            }
        }

        public String MsgId
        {
            get
            {
                return GetHeadOrParam(HEADER_MSGID);
            }
            set
            {
                SetHead(HEADER_MSGID, value);

            }
        }

        public String MsgIdRaw
        {
            get
            {
                return GetHeadOrParam(HEADER_MSGID_RAW);
            }
            set
            {
                SetHead(HEADER_MSGID_RAW, value);

            }
        }
        public String Token
        {
            get
            {
                return GetHeadOrParam(HEADER_TOKEN);
            }
            set
            {
                SetHead(HEADER_TOKEN, value);

            }
        }
        public String Topic
        {
            get
            {
                return GetHeadOrParam(HEADER_TOPIC);
            }
            set
            {
                SetHead(HEADER_TOPIC, value);

            }
        }

        public String Encoding
        {
            get
            {
                return GetHeadOrParam(HEADER_ENCODING);
            }
            set
            {
                SetHead(HEADER_ENCODING, value);

            }
        }

        public bool Ack
        {
            get
            {
                string ack = GetHeadOrParam(HEADER_ACK);
                if (ack == null) return true; //default to true
                return "1".Equals(ack);
            }
            set
            {
                string val = value ? "1" : "0";
                SetHead(HEADER_ACK, val);
            }
        }

        public string Uri
        {
            get
            {
                return this.meta.Uri;
            }
        }

        public string Path
        {
            get
            {
                return this.meta.Path;
            }
        }

        public string Status
        {
            get
            {
                return this.meta.Status;
            }
            set
            { 
                this.meta.Status = value;
            }
        }

        public bool IsStatus200()
        {
            return "200".Equals(this.meta.Status);
        }
        public bool IsStatus404()
        {
            return "404".Equals(this.meta.Status);
        }
        public bool IsStatus500()
        {
            return "500".Equals(this.meta.Status);
        }

#endregion

        public void Encode(IoBuffer buf)
        {
            buf.Put("{0}\r\n", this.meta);
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

        private static int FindHeaderEnd(IoBuffer buf)
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
            msg.meta = new Meta(lines[0]);
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

        public static Message Decode(IoBuffer buf)
        {
            int idx = FindHeaderEnd(buf);
            int headLen = idx - buf.Position+1;
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
            IoBuffer buf = new IoBuffer();
            Encode(buf);
            return buf.ReadAllToString();
        } 
    }
}
