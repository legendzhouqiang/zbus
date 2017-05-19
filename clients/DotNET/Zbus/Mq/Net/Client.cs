using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Security;
using System.Net.Sockets;
using System.Security.Cryptography.X509Certificates;
using System.Threading;
using System.Threading.Tasks;
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
        public Client(string serverAddress, ICodec codecRead, ICodec codecWrite) :
            this(new ServerAddress(serverAddress), codecRead, codecWrite)
        {

        }
        public Client(string serverAddress, ICodec codec)
            : this(serverAddress, codec, codec)
        {
        }
        
        public async Task ConnectAsync()
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
            await this.tcpClient.ConnectAsync(host, port);
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
         
        public async Task<RES> InvokeAsync(REQ req, CancellationToken? token=null)
        {
            await SendAsync(req, token);
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

                res = await RecvAsync(token);
                if (res.Id == reqId) return res;

                resultTable[res.Id] = res;
            }
        }
         
        public async Task SendAsync(REQ req, CancellationToken? token=null)
        {
            if(token == null)
            {
                token = CancellationToken.None;
            }
            if (req.Id == null)
            {
                req.Id = Guid.NewGuid().ToString();
            }
            ByteBuffer buf = this.codecWrite.Encode(req);
            await stream.WriteAsync(buf.Data, 0, buf.Limit, token.Value);
            await stream.FlushAsync(token.Value);
        }
         
        public async Task<RES> RecvAsync(CancellationToken? token=null)
        {
            if (token == null)
            {
                token = CancellationToken.None;
            }
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
                int n = await stream.ReadAsync(buf, 0, buf.Length, token.Value);
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
        public Client(string serverAddress, ICodec codec) 
            : base(serverAddress, codec) { }
        public Client(ServerAddress serverAddress, ICodec codec, string certFile = null)
            : base(serverAddress, codec, codec, certFile) { }
    }
}
