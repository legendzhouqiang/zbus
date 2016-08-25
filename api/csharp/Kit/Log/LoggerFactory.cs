using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Diagnostics;

namespace Zbus.Kit.Log
{ 
    public class LoggerFactory
    { 
        public static ILogger GetLogger(string name)
        {
            return new ConsoleLogger(name);
        }

        public static ILogger GetLogger(Type type)
        {
            return GetLogger(type.Name);
        }
    }
}
