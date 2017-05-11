using NUnit.Framework;
using Ploeh.AutoFixture;
using FluentAssertions;
using Zbus.Client.Net; 

namespace Zbus.Client.Test.Net
{ 
   public class IoBuffer_Tests
   {
      private IFixture fixture;
      [SetUp]
      public void TestSetup()
      {
         fixture = new Fixture();
      }


      [Test]
      public void Test_IoBuffer_Size_From0([Range(0,0)]int size)
      {
         //Arrange   
         IoBuffer buf = new IoBuffer(size);

         //Act 

         //Assert
         buf.Capacity.Should().Be(size);
      }

      [Test]
      public void Test_IoBuffer_Size([Random(0, 1024*1024*10, 10)]int size)
      {
         //Arrange   
         IoBuffer buf = new IoBuffer(size);

         //Act 

         //Assert
         buf.Capacity.Should().Be(size);
      }


      [Test]
      public void Test_Duplicate([Random(0, 1024 * 1024 * 10, 10)]int size)
      {
         //Arrange   
         IoBuffer buf = new IoBuffer(size);
         buf.Put(new byte[size / 2]);

         //Act 
         IoBuffer buf2 = buf.Duplicate();

         //Assert
         buf2.ShouldBeEquivalentTo(buf);
      } 
   }
}
