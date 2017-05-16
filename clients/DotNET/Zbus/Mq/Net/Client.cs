using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Cryptography.X509Certificates;
using Zbus.Mq; 

namespace Zbus.Mq.Net
{

    /// <summary>
    /// Identity interface to track message match for asynchroneous invocation.
    /// </summary>
    public interface Id
    {
        /// <summary>
        /// Identity string
        /// </summary>
        string Id { get; set; }
    } 
    

    public class Client<REQ, RES> : IDisposable where REQ : Id where RES : Id
    {
        public bool AllowSelfSignedCertficate { get; set; }
        private readonly TcpClient tcpClient;
        private ICodec codecRead;
        private ICodec codecWrite;

        private ServerAddress serverAddress;
        private string certFile;

        private ByteBuffer readBuf = new ByteBuffer();
        private IDictionary<string, RES> resultTable = new ConcurrentDictionary<string, RES>();

        private Stream stream;

        public Client(ServerAddress serverAddress, ICodec codecRead, ICodec codecWrite, string certFile = null)
        {
            this.serverAddress = serverAddress;
            this.tcpClient = new TcpClient();
            this.tcpClient.NoDelay = true;
            this.codecRead = codecRead;
            this.codecWrite = codecWrite;
            this.certFile = certFile;
        }
        public Client(string serverAddress, ICodec codecRead, ICodec codecWrite, string certFile = null) :
            this(new ServerAddress(serverAddress), codecRead, codecWrite, certFile)
        {

        }
        public Client(string serverAddress, ICodec codec, string certFile = null)
            : this(serverAddress, codec, codec, certFile)
        {
        }

        public void Connect()
        {
            string[] bb = this.serverAddress.Address.Trim().Split(':');
            string host;
            int port = 80;
            if (bb.Length < 2)
            {
                host = bb[0];
            }
            else
            {
                host = bb[0];
                port = int.Parse(bb[1]);
            }
            this.tcpClient.Connect(host, port);
            this.stream = this.tcpClient.GetStream();

            if (this.serverAddress.SslEnabled)
            {
                if (this.certFile == null)
                {
                    throw new ArgumentException("Missing certificate file");
                }
                X509Certificate cert = X509Certificate.CreateFromCertFile(this.certFile);
                SslStream sslStream = new SslStream(this.stream, false,
                    new RemoteCertificateValidationCallback(ValidateServerCertificate), null);
                sslStream.AuthenticateAsClient(this.serverAddress.Address);
                this.stream = sslStream;
            }
        }

        bool ValidateServerCertificate(object sender, X509Certificate certificate, X509Chain chain, SslPolicyErrors sslPolicyErrors)
        {
            if (AllowSelfSignedCertficate) return true;
            if (sslPolicyErrors == SslPolicyErrors.None)
            {
                return true;
            }
            Console.WriteLine("Certificate error: {0}", sslPolicyErrors);
            return false;
        }

        public RES Invoke(REQ req, int timeout = 3000)
        {
            Send(req, timeout);
            string reqId = req.Id;
            RES res;
            while (true)
            {
                if (resultTable.ContainsKey(reqId))
                {
                    res = resultTable[reqId];
                    resultTable.Remove(reqId);
                    return res;
                }

                res = Recv(timeout);
                if (res.Id == reqId) return res;

                resultTable[res.Id] = res;
            }
        }

        public void Send(REQ req, int timeout = 3000)
        {
            if (req.Id == null)
            {
                req.Id = Guid.NewGuid().ToString();
            }
            stream.WriteTimeout = timeout;
            ByteBuffer buf = this.codecWrite.Encode(req);
            stream.Write(buf.Data, 0, buf.Limit);
            stream.Flush();
        }

        public RES Recv(int timeout = 3000)
        {
            stream.ReadTimeout = timeout;

            byte[] buf = new byte[4096];
            while (true)
            {
                ByteBuffer tempBuf = this.readBuf.Duplicate();
                tempBuf.Flip(); //to read mode
                object msg = codecRead.Decode(tempBuf);
                if (msg != null)
                {
                    this.readBuf.Move(tempBuf.Position);
                    RES res = (RES)msg;
                    return res;
                }
                int n = stream.Read(buf, 0, buf.Length);
                this.readBuf.Put(buf, 0, n);
            }
        }

        public void Dispose()
        {
            if (stream != null)
            {
                stream.Close();
            }
            if (this.tcpClient != null)
            {
                this.tcpClient.Close();
            }
        }

        public bool Connected
        {
            get
            {
                return this.tcpClient != null && this.tcpClient.Connected;
            }
        }
    }


    public class Client<T> : Client<T, T> where T : Id
    {
        public Client(string serverAddress, ICodec codec, string certFile = null) 
            : base(serverAddress, codec, certFile) { }
        public Client(ServerAddress serverAddress, ICodec codec, string certFile = null)
            : base(serverAddress, codec, codec, certFile) { }
    }
}
