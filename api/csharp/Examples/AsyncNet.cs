using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace examples
{
    class AsyncNet
    {
        public static void Main(string[] args)
        {
            AsyncNetTest();
            Console.WriteLine("===Main==="); 
            Thread.Sleep(6000000);
            Console.ReadKey();
        } 


        public static async void AsyncNetTest()
        { 
            CancellationTokenSource cts = new CancellationTokenSource();
            TcpListener listener = new TcpListener(IPAddress.Any, 6666);
            try
            {
                listener.Start();
                Console.WriteLine("Server started: {0}", listener.LocalEndpoint);
                await AcceptClientAsync(listener, cts.Token); 
            }
            finally
            {
                cts.Cancel();
                listener.Stop();
            }
            Console.WriteLine("Server stopped"); 
        }

        static async Task AcceptClientAsync(TcpListener listener, CancellationToken ct)
        {
            var clientCounter = 0;
            while (!ct.IsCancellationRequested)
            {
                TcpClient client = await listener.AcceptTcpClientAsync();
                clientCounter++;
                try { 
                    EchoAsync(client, clientCounter, ct);
                }
                catch (Exception e)
                {
                    Console.WriteLine(e);
                }
                Console.WriteLine("Continue to wait for another client");
            } 
        }

        static async void EchoAsync(TcpClient client, int clientIndex, CancellationToken ct)
        {
            Console.WriteLine("New Client {0} connected", clientIndex);
            using (client)
            {
                var buf = new byte[4096];
                var stream = client.GetStream();
                while (!ct.IsCancellationRequested)
                {
                    var timeoutTask = Task.Delay(TimeSpan.FromSeconds(5*60));
                    var amountReadTask = stream.ReadAsync(buf, 0, buf.Length, ct); 
                    var completedTask = await Task.WhenAny(timeoutTask, amountReadTask).ConfigureAwait(false);
                    if(completedTask == timeoutTask)
                    {
                        var msg = Encoding.ASCII.GetBytes("Client timeout");
                        await stream.WriteAsync(msg, 0, msg.Length);
                        break;
                    }

                    var amountRead = amountReadTask.Result;
                    if (amountRead == 0) break;
                    Console.Write(Encoding.Default.GetString(buf, 0, amountRead));
                    await stream.WriteAsync(buf, 0, amountRead, ct).ConfigureAwait(false);
                }
            }
            Console.WriteLine("Client {0} disconnected", clientIndex);
        }
    }
}
