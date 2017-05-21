using System;
using System.Threading;
using System.Threading.Tasks;
using Zbus.Mq;

namespace Zbus.Examples
{
    class MultiThreadingAsyncExample
    {
        static async Task Test()
        {
            SemaphoreSlim s = new SemaphoreSlim(1);
            int x = 0;
            for(int i=0;i<1000;i++)
            await Task.Run(async () =>
            {
                await s.WaitAsync();
                x++; 
                s.Release();
            });

            Thread.Sleep(1000);
            Console.WriteLine(x);
            
        }
        static void Main(string[] args)
        { 
            Test().Wait();
            Console.ReadKey();
        }
    }
}
