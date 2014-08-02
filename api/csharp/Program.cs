using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using fastJSON;

namespace zbus
{
    class Program
    {
        static void Main(string[] args)
        {
            Dictionary<string, object> req = new Dictionary<string, object>();
            req["method"] = "m";
            req["params"] = "args";

            string json = JSON.Instance.ToJSON(req);
            Console.WriteLine(json);
            Console.ReadKey();
        }
    }
}
