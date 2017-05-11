using NUnit.Framework;
using Ploeh.AutoFixture;
using FluentAssertions;
using Zbus.Client.Rpc;
using Zbus.Client.Broker;
using Api.Example;
using Zbus.Client.Mq;

namespace Zbus.Client.Test.Rpc
{ 
   public class Rpc_Tests
   {
      private IBroker broker;
      private ConsumerService service;
      private IFixture fixture;

      [SetUp]
      public void Setup()
      {
         fixture = new Fixture();
         broker = new ZbusBroker("127.0.0.1:15555");

         RpcProcessor rpcProcessor = new RpcProcessor();
         rpcProcessor.AddModule<MyService>();
          
         ConsumerServiceConfig config = new ConsumerServiceConfig
         {
            Mq = "MyRpc",
            Broker = broker,
            ConsumerCount = 4,
            ConsumerMessageHandler = rpcProcessor.OnConsumerMessage,
         };

         service = new ConsumerService(config);
         service.Start();
      }

      [TearDown]
      public void Teardown()
      {
         broker.Dispose();
         service.Stop();
      }


      [Test]
      public void Test_Message()
      {
         //Arrange    

         //Act 

         //Assert 
      } 
       
   }
}
