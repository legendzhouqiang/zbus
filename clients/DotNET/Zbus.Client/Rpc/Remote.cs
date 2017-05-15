using System;

namespace Zbus.Client.Rpc
{

    public class Remote : Attribute
    {
        public string Id { get; set; }
        public bool Exclude { get; set; }

        public Remote()
        {
            Id = null;
        }

        public Remote(string id)
        {
            this.Id = id;
        }

        public Remote(bool exclude)
        {
            this.Exclude = exclude;
        }
    }
}