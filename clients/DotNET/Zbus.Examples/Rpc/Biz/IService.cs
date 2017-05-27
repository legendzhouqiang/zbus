using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Api.Example
{

    public interface IService
    {
        string Echo(string msg);

        Task<string> GetStringAsync();

        void NoReturn();

        int Plus(int a, int b);

        Task<int> PlusAsync(int a, int b);

        void ThrowException();
    }
}
