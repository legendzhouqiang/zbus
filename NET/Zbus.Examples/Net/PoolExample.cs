using log4net;
using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Net;
using Zbus.Net.Http;

namespace Zbus.Examples.Net
{
   public class PoolExample
   {
      static readonly ILog log = LogManager.GetLogger(typeof(PoolExample));

      public static void Main(string[] args)
      {
         DateTime start = DateTime.Now;
         Console.WriteLine("Start: {0}", start);
         Pool<MessageClient> pool = new Pool<MessageClient>
         {
            MaxCount = 64,
            Generator = () =>
            {
               MessageClient client = new MessageClient();
               client.ConnectAsync("127.0.0.1", 8080).Wait();
               client.StartHeartbeat(10000);
               return client;
            },
         };

         Task[] tasks = new Task[8];
         int totalInvoke = 0;
         for (int i = 0; i < tasks.Length; i++)
         {
            tasks[i] = Task.Run(async () =>
            {
               for (int j = 0; j < 1000; j++)
               {
                  Message msg = new Message();
                  msg.Url = "/";
                  msg.BodyString = "Hello World " + i;

                  Interlocked.Increment(ref totalInvoke);
                  MessageClient client = pool.Borrow();
                  Message res = await client.InvokeAsync(msg);
                  pool.Return(client);

                  log.Info(res);
                  //Console.WriteLine(res);
               }
            });
         }
         foreach (Task task in tasks)
         {
            task.Wait();
         }

         DateTime end = DateTime.Now;
         Console.WriteLine("Pool.Count={0}, Cost={1}, Invoke={2}", pool.Count, end.Subtract(start), totalInvoke);
         Console.ReadKey();

         pool.Dispose();
      }
   }
}
