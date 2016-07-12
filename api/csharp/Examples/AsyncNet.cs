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
         Thread.Sleep(1000000);
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

      static int clientCounter = 0;

      struct ClientContext
      {
         public TcpClient Client { get; set; }
         public CancellationToken Token { get; set; }
      }
      static async Task AcceptClientAsync(TcpListener listener, CancellationToken ct)
      {
         while (!ct.IsCancellationRequested)
         {
            TcpClient client = await listener.AcceptTcpClientAsync();
            clientCounter++;
            ThreadPool.QueueUserWorkItem(ClientHandler, new ClientContext { Client = client, Token = ct });
            Console.WriteLine("Continue to wait for another client");
         }
      }

      static void ClientHandler(object context)
      {
         ClientContext ctx = (ClientContext)context;
         try
         {
            EchoAsync(ctx.Client, clientCounter, ctx.Token);
         }
         catch (Exception e)
         {
            Console.WriteLine(e);
         }
      }

      static async void EchoAsync(TcpClient client, int clientIndex, CancellationToken ct)
      {
         Console.WriteLine("New Client {0} connected", clientIndex);
         try
         {
            using (client)

            {
               var buf = new byte[4096];
               var stream = client.GetStream();
               while (!ct.IsCancellationRequested)
               {
                  var timeoutCTS = new CancellationTokenSource();
                  var timeoutTask = Task.Delay(TimeSpan.FromSeconds(5 * 60), timeoutCTS.Token);

                  var amountReadTask = stream.ReadAsync(buf, 0, buf.Length, ct);
                  var completedTask = await Task.WhenAny(timeoutTask, amountReadTask).ConfigureAwait(false);
                  if (completedTask == timeoutTask)
                  {
                     var msg = Encoding.ASCII.GetBytes("Client timeout");
                     await stream.WriteAsync(msg, 0, msg.Length);
                     break;
                  }
                  timeoutCTS.Cancel();

                  var amountRead = amountReadTask.Result;
                  if (amountRead == 0) break;
                  Console.Write(Encoding.Default.GetString(buf, 0, amountRead));
                  await stream.WriteAsync(buf, 0, amountRead, ct).ConfigureAwait(false);
               }
            }
         }
         catch (Exception e)
         {
            Console.WriteLine(e);

         }
         Console.WriteLine("Client {0} disconnected", clientIndex);
      }
   }
}
