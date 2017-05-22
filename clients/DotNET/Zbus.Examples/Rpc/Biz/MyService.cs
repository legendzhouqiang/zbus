using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks; 
namespace Api.Example
{
    public class MyService : IService
    {
        public string Echo(string msg)
        {
            return msg;
        }

        public Task<string> GetStringAsync()
        {
            return Task.Run(() =>
            {
                Thread.Sleep(100);
                return "Sleep(100)";
            });
        } 

        public void NoReturn()
        {

        }

        public int Plus(int a, int b)
        {
            return a + b;
        }

        public Task<int> PlusAsync(int a, int b)
        {
            return Task.Run(() =>
            {
                return a + b;
            });
        }

        public void ThrowException()
        {
            throw new NotImplementedException();
        }
    }
}
