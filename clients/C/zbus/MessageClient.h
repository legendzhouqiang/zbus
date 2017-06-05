#ifndef __ZBUS_MESSAGE_CLIENT_H__
#define __ZBUS_MESSAGE_CLIENT_H__  
 
#include "Platform.h"
#include "Message.h"
#include "Logger.h"
#include "Buffer.h";

#include <map>
using namespace std;


#define ERR_NET_UNKNOWN_HOST        -86  /**< Failed to get an IP address for the given hostname. */
#define ERR_NET_SOCKET_FAILED       -66  /**< Failed to open a socket. */
#define ERR_NET_CONNECT_FAILED      -68  /**< The connection to the given server / port failed. */
#define ERR_NET_BIND_FAILED         -70  /**< Binding of the socket failed. */
#define ERR_NET_LISTEN_FAILED       -72  /**< Could not listen on the socket. */
#define ERR_NET_ACCEPT_FAILED       -74  /**< Could not accept the incoming connection. */
#define ERR_NET_RECV_FAILED         -76  /**< Reading information from the socket failed. */
#define ERR_NET_SEND_FAILED         -78  /**< Sending information through the socket failed. */
#define ERR_NET_CONN_RESET          -80  /**< Connection was reset by peer. */
#define ERR_NET_WANT_READ           -82  /**< Connection requires a read call. */
#define ERR_NET_WANT_WRITE          -84  /**< Connection requires a write call. */

#define NET_LISTEN_BACKLOG           10  /**< The backlog that listen() should use. */
 

ZBUS_API class MessageClient {
private:
	Logger* logger;
private:
	int socket;

	string address;
	bool sslEnabed;

	string sslCertFile;
	map<string, Message*> msgTable;
	ByteBuffer* readBuffer;
private:
	void resetReadBuffer();
public:
	ZBUS_API MessageClient(string address, bool sslEnabled=false, string sslCertFile="");
	ZBUS_API virtual ~MessageClient();

	ZBUS_API int connect();
	ZBUS_API int send(Message& msg, int timeout=3000);
	ZBUS_API Message* recv(int& rc, const char* msgid=NULL, int timeout=3000);
};

#endif