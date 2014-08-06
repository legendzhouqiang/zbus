using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.IO;
using System.Net;
using System.Net.Sockets;
using fastJSON;

using System.Threading;

using zbus.Remoting;

namespace zbus
{

    class Program
    {
        static void Main2(string[] args)
        { 
            string req = "GET /produce?mq=MyMQ\r\n"+
                "msgid: f11fdad0-5111-4e45-bdcc-f30e70810966\r\n"+
                "content-length: 11\r\n"+
                "\r\n"+
                "hongleiming";

            RemotingClient client = new RemotingClient("127.0.0.1:15555");
            client.Send(req);
            string res = client.Recv();
            Console.Write(res);
         
            Console.ReadKey();
        }
    }
}
