using System;
using Zbus.Net.Http;
using Zbus.Net;

namespace Zbus.Examples.Net
{ 
   class NetExample
   {
      public static void Main(string[] args)
      {
         PoolTest();
         Console.WriteLine("====main=====");
         Console.ReadKey();
      }

      static async void PoolTest()
      { 
         Pool<MessageClient> pool = new Pool<MessageClient>
         {
            MaxCount = 64,
            Generator = ()=>
            {
               MessageClient client = new MessageClient();
               client.ConnectAsync("127.0.0.1", 8080);
               client.StartHeartbeat(10000);
               return client;
            },
         };  


         for (int i = 0; i < 100; i++)
         {
            Message msg = new Message();
            msg.Url = "/";
            msg.BodyString = "Hello World " + i;

            MessageClient client = pool.Borrow();
            Message res = await client.InvokeAsync(msg);
            pool.Return(client);

            Console.WriteLine(res);
         }
         Console.WriteLine("\n\n====done=====");
         pool.Dispose();

      }

      static async void Test()
      {

         //ICodec codec = new MessageCodec();
         //Client<Message> client = new Client<Message>(codec); 
         MessageClient client = new MessageClient();
         client.StartHeartbeat(1000);
         await client.ConnectAsync("127.0.0.1", 8080); 

         for (int i = 0; i < 10; i++) 
         {
            Message msg = new Message();
            msg.Url = "/";
            msg.BodyString = "Hello World " + i;

            Message res = await client.InvokeAsync(msg);
            Console.WriteLine(res); 
         } 

         client.Dispose();
      }
   }
}
