using System; 
using System.Diagnostics; 
using System.IO;


namespace Zbus.Examples
{
    class MyTraceListener : TraceListener
    {
        public override void Write(string message)
        {
            File.AppendAllText("d:\\1.log", message);
        }

        public override void WriteLine(string message)
        {
            File.AppendAllText("d:\\1.log", DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss    ") + message + Environment.NewLine);
        }
    }

    class LogTest
    {
        public static void Main(string[] args)
        {
            Trace.Listeners.Clear();
            Trace.Listeners.Add(new ConsoleTraceListener());
            Trace.Listeners.Add(new MyTraceListener());
            Trace.TraceError("error");
            Console.WriteLine("--done--");
            Console.ReadKey();
        }
    }
}
