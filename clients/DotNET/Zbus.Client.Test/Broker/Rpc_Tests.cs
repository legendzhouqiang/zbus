using NUnit.Framework;
using Ploeh.AutoFixture;
using FluentAssertions;
using Zbus.Client.Rpc;
using Zbus.Client.Broker;
using Api.Example;
using Zbus.Client.Mq;
using System;
using Zbus.Client.Net.Http;

namespace Zbus.Client.Test.Broker
{ 
   public class Broker_Tests
   {
      [Test]
      public void Test_Broker()
      {
         //Arrange   
         BrokerConfig config = new BrokerConfig
         {
            BrokerAddress = "127.0.0.1:15555",
            PoolSize = 8,
         };
         IBroker broker = new ZbusBroker(config);

         //Act 
         var list = new System.Collections.ArrayList();
         for (int i = 0; i < 8; i++)
         { 
            var invoker = broker.GetInvoker(new ClientHint());
            list.Add(invoker);
         }
          
         //Assert
         list.Count.ShouldBeEquivalentTo(8);

         broker.Dispose();
      }


      [Test]
      public void Test_Broker_Exception()
      {
         //Arrange    
         IBroker broker = new ZbusBroker("127.0.0.1:15555");

         //Act  
         broker.Dispose(); 

         //Assert  
         Action action = () => broker.GetInvoker(new ClientHint());
         action.ShouldThrow<Exception>();
      }
   }
}
