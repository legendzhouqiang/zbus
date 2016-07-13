using System;

namespace Zbus.RPC
{
   public class Remote : Attribute
   {
      private string id; 
      private bool exclude = false;


      public Remote()
      {
         this.id = null;
      }

      public Remote(string id)
      {
         this.id = id;
      }

      public Remote(bool exclude)
      {
         this.exclude = exclude;
      }

      public string Id
      {
         get { return this.id; }
         set { this.id = value; }
      }

      public bool Exclude
      {
         get { return exclude; }
         set { exclude = value; }
      }
   }

}
