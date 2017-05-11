using System; 

namespace Zbus.Client.Net
{
   /// <summary>
   /// Abastraction of Connection between client and server
   /// </summary>
   public interface ISession : IDisposable
   { 
      /// <summary>
      /// Write a message to the session, may be a socket write
      /// </summary>
      /// <param name="msg">message object to write, no need to encode, and will be encoded</param>
      /// <param name="timeout">timeout in milliseconds to write</param>
      void Write(object msg, int timeout = 10000);
      /// <summary>
      /// Flush the underlying media if there is cache
      /// </summary>
      void Flush();
      /// <summary>
      /// Write a message and flush to the session, may be a socket write
      /// </summary>
      /// <param name="msg">message object to write, no need to encode, and will be encoded</param>
      /// <param name="timeout">timeout in milliseconds to write</param>
      void WriteAndFlush(object msg, int timeout = 10000);
      /// <summary>
      /// Read an encoded object from session
      /// </summary>
      /// <param name="timeout">timeout in milliseconds to read</param>
      /// <returns></returns>
      object Read(int timeout = 10000);


      #region Session Properties
      /// <summary>
      /// Id of the session
      /// </summary>
      string Id { get; }
      /// <summary>
      /// Remote address of the session, usually the remote socket address
      /// </summary>
      string RemoteAddress { get; }
      /// <summary>
      /// Local address of the session, usually the local socket address
      /// </summary>
      string LocalAddress { get; }
      /// <summary>
      /// Indication of the current status of the session, true when connected and no error
      /// </summary>
      bool Active { get; }
      /// <summary>
      /// Read the attached attribute in this session
      /// </summary>
      /// <typeparam name="V">type of the attribute value</typeparam>
      /// <param name="key">attribute key</param>
      /// <returns>Attribute value object</returns>
      V Attr<V>(string key);
      /// <summary>
      /// Attach a key-value pair to this session
      /// </summary>
      /// <typeparam name="V">type of attribute value</typeparam>
      /// <param name="key">attribute key</param>
      /// <param name="value">attribute value object</param>
      void Attr<V>(string key, V value);

      #endregion
   }
}
