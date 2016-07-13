using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zbus.Examples.RPC
{
   class JsonExample
   {
      class Person
      {
         public string Name { get; set; }
         public int Age { get; set; }

         public override string ToString()
         {
            return string.Format("Name:{0}, Age:{1}", Name, Age);
         }
      }
      public static void Main(string[] args)
      {
         IDictionary<string, object> dict = new Dictionary<string, object>();
         dict["Name"] = "HONG";
         dict["Age"] = 18;
          

         string res = JsonConvert.SerializeObject(dict);
         Console.WriteLine(res);
         Person p = JsonConvert.DeserializeObject<Person>(res);
         Console.WriteLine(p); 

         Console.ReadKey();
      }
   }
}
