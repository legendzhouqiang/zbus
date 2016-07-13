namespace Zbus.Mq
{
   public enum MqMode
   {
      MQ = 1 << 0,
      PubSub = 1 << 1,
      Memory = 1 << 2,
      RPC = 1 << 3,
   }

   public class Proto
   {
      public static readonly string Produce = "produce";
      public static readonly string Consume = "consume";
      public static readonly string Route = "route";
      public static readonly string Heartbeat = "heartbeat";
      public static readonly string CreateMQ = "create_mq";
   }
}
