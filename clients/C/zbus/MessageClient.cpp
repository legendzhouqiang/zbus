#include "Platform.h"
#include "Buffer.h"
#include "MessageClient.h" 

#ifdef __WINDOWS__
#include <objbase.h>
#else
#include <uuid/uuid.h>
typedef struct _GUID
{
	unsigned long  Data1;
	unsigned short Data2;
	unsigned short Data3;
	unsigned char  Data4[8];
} GUID, UUID;

#endif

static void gen_uuid(char* buf, int len) {
	UUID uuid;
#ifdef __WINDOWS__
	CoCreateGuid(&uuid);
#else
	uuid_generate((char*)&uuid);
#endif

	snprintf(buf, len, "%08x-%04x-%04x-%02x%02x-%02x%02x%02x%02x%02x%02x",
		uuid.Data1, uuid.Data2, uuid.Data3,
		uuid.Data4[0], uuid.Data4[1],
		uuid.Data4[2], uuid.Data4[3],
		uuid.Data4[4], uuid.Data4[5],
		uuid.Data4[6], uuid.Data4[7]);
}


#ifdef __WINDOWS__

#define read(fd,buf,len)        recv(fd,(char*)buf,(int) len,0)
#define write(fd,buf,len)       send(fd,(char*)buf,(int) len,0)
#define close(fd)               closesocket(fd)

static int wsa_init_done = 0;
#endif


#define HTONS(n) ((((unsigned short)(n) & 0xFF      ) << 8 ) | \
                           (((unsigned short)(n) & 0xFF00    ) >> 8 ))
#define HTONL(n) ((((unsigned long )(n) & 0xFF      ) << 24) | \
                           (((unsigned long )(n) & 0xFF00    ) << 8 ) | \
                           (((unsigned long )(n) & 0xFF0000  ) >> 8 ) | \
                           (((unsigned long )(n) & 0xFF000000) >> 24))

unsigned short net_htons(unsigned short n);
unsigned long  net_htonl(unsigned long  n);
#define net_htons(n) HTONS(n)
#define net_htonl(n) HTONL(n)


/*
* Set the socket blocking or non-blocking
*/
static int net_set_block(int fd)
{
#ifdef __WINDOWS__
	u_long n = 0;
	return(ioctlsocket(fd, FIONBIO, &n));
#else
	return(fcntl(fd, F_SETFL, fcntl(fd, F_GETFL) & ~O_NONBLOCK));
#endif
}

static int net_set_nonblock(int fd)
{
#ifdef __WINDOWS__
	u_long n = 1;
	return(ioctlsocket(fd, FIONBIO, &n));
#else
	return(fcntl(fd, F_SETFL, fcntl(fd, F_GETFL) | O_NONBLOCK));
#endif
}

/*
* Prepare for using the sockets interface
*/
static int net_prepare(void)
{
#ifdef __WINDOWS__ 
	if (wsa_init_done == 0)
	{
		WSADATA wsaData;
		if (WSAStartup(MAKEWORD(2, 0), &wsaData) == SOCKET_ERROR)
			return(ERR_NET_SOCKET_FAILED);

		wsa_init_done = 1;
	}
#else
	signal(SIGPIPE, SIG_IGN);
#endif
	return(0);
}

//connect error, to fix
static int connect_with_timeout(int fd, struct sockaddr *name, int len, int64_t timeout) {
	fd_set wset, eset;
	int rc;
	struct timeval tv;

	if (timeout <= 0) {//no handling
		return connect(fd, name, len);
	}

	FD_ZERO(&wset);
	FD_ZERO(&eset);
	FD_SET(fd, &wset);
	FD_SET(fd, &eset);

	tv.tv_sec = (long)timeout / 1000;
	tv.tv_usec = timeout % 1000 * 10000;

	rc = net_set_nonblock(fd);
	if (rc != 0) {
		return rc;
	}
	rc = connect(fd, name, len);
	if (rc != 0) {
		return rc;
	}
	net_set_block(fd);

	select(0, NULL, &wset, &eset, &tv);

	if (FD_ISSET(fd, &wset)) {
		return 0;
	}
	return -1;
}

static void net_close(int fd)
{
	shutdown(fd, 2);
	close(fd);
}


#ifdef __WINDOWS__
/*
* Check if the requested operation would be blocking on a non-blocking socket
* and thus 'failed' with a negative return value.
*/
static int net_would_block(int fd)
{
	return(WSAGetLastError() == WSAEWOULDBLOCK);
}
#else
/*
* Check if the requested operation would be blocking on a non-blocking socket
* and thus 'failed' with a negative return value.
*
* Note: on a blocking socket this function always returns 0!
*/
static int net_would_block(int fd)
{
	/*
	* Never return 'WOULD BLOCK' on a non-blocking socket
	*/
	if ((fcntl(fd, F_GETFL) & O_NONBLOCK) != O_NONBLOCK)
		return(0);

	switch (errno)
	{
#if defined EAGAIN
	case EAGAIN:
#endif
#if defined EWOULDBLOCK && EWOULDBLOCK != EAGAIN
	case EWOULDBLOCK:
#endif
		return(1);
	}
	return(0);
}
#endif


static int net_set_timeout(int fd, int64_t timeout) {
	int rc = -1;
#ifdef __UNIX__
	struct timeval tv;
	tv.tv_sec = (long)(timeout / 1000);
	tv.tv_usec = timeout % 1000 * 10000;
	rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&tv, sizeof(tv));
#endif

#ifdef __WINDOWS__
	rc = setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, (const char*)&timeout, sizeof(timeout));
#endif 
	return rc;
}  
/*
* Initiate a TCP connection with host:port
*/
static int net_connect(int *fd, const char *host, int port)
{
	int ret;
	struct addrinfo hints, *addr_list, *cur;
	char port_str[6];

	if ((ret = net_prepare()) != 0)
		return(ret);

	/* getaddrinfo expects port as a string */
	memset(port_str, 0, sizeof(port_str));
	snprintf(port_str, sizeof(port_str), "%d", port);

	/* Do name resolution with both IPv6 and IPv4, but only TCP */
	memset(&hints, 0, sizeof(hints));
	hints.ai_family = AF_UNSPEC;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;

	if (getaddrinfo(host, port_str, &hints, &addr_list) != 0)
		return(ERR_NET_UNKNOWN_HOST);

	/* Try the sockaddrs until a connection succeeds */
	ret = ERR_NET_UNKNOWN_HOST;
	for (cur = addr_list; cur != NULL; cur = cur->ai_next) {
		*fd = (int)socket(cur->ai_family, cur->ai_socktype,
			cur->ai_protocol);
		if (*fd < 0) {
			ret = ERR_NET_SOCKET_FAILED;
			continue;
		}
		if (connect(*fd, cur->ai_addr, cur->ai_addrlen) == 0) {
			ret = 0;
			break;
		}

		close(*fd);
		ret = ERR_NET_CONNECT_FAILED;
	}

	freeaddrinfo(addr_list);

	return(ret);
}



/*
* Read at most 'len' characters
*/
static int net_recv(int fd, unsigned char *buf, size_t len)
{
	int ret = read(fd, buf, len);

	if (ret < 0)
	{
		if (net_would_block(fd) != 0)
			return(ERR_NET_WANT_READ);

#ifdef __WINDOWS__
		if (WSAGetLastError() == WSAECONNRESET)
			return(ERR_NET_CONN_RESET);
#else
		if (errno == EPIPE || errno == ECONNRESET)
			return(ERR_NET_CONN_RESET);

		if (errno == EINTR)
			return(ERR_NET_WANT_READ);
#endif

		return(ERR_NET_RECV_FAILED);
	}

	return(ret);
}

/*
* Write at most 'len' characters
*/
static int net_send(int fd, const unsigned char *buf, size_t len)
{
	int ret = write(fd, buf, len);

	if (ret < 0)
	{
		if (net_would_block(fd) != 0)
			return(ERR_NET_WANT_WRITE);

#ifdef __WINDOWS__
		if (WSAGetLastError() == WSAECONNRESET)
			return(ERR_NET_CONN_RESET);
#else
		if (errno == EPIPE || errno == ECONNRESET)
			return(ERR_NET_CONN_RESET);

		if (errno == EINTR)
			return(ERR_NET_WANT_WRITE);
#endif

		return(ERR_NET_SEND_FAILED);
	}

	return(ret);
}
 
MessageClient::MessageClient(string serverAddress, string sslCertFile) :
	serverAddress(serverAddress),
	sslCertFile(sslCertFile),
	socket(-1)
{ 
	logger = Logger::getLogger();
	readBuffer = NULL;
}
MessageClient::MessageClient(ServerAddress& serverAddress, string sslCertFile) :
	serverAddress(serverAddress),
	sslCertFile(sslCertFile),
	socket(-1)
{
	logger = Logger::getLogger();
	readBuffer = NULL;
}


MessageClient::~MessageClient() {
	printf("destroy\n");
	if (socket != -1) {
		net_close(socket);
		socket = -1;
	}
	for (map<string, Message*>::iterator iter = msgTable.begin(); iter != msgTable.end(); iter++) {
		delete iter->second;
	}
	msgTable.clear();
	if (readBuffer) {
		delete readBuffer;
		readBuffer = NULL;
	}
}
void MessageClient::resetReadBuffer() {
	if (readBuffer) {
		delete readBuffer; 
	}
	readBuffer = new ByteBuffer(10240);
}
int MessageClient::connect() {
	resetReadBuffer();
	string address = this->serverAddress.address;
	size_t pos = address.find(':');
	int port = 80; 
	char* host = (char*)address.substr(0, pos).c_str();
	if (pos != -1) {
		port = atoi(address.substr(pos + 1).c_str());
	}  
	if(logger->isDebugEnabled()){
		logger->debug("Trying connect to (%s)", address.c_str());
	}
	int ret = net_connect(&this->socket, host, port); 
	if (ret == 0) {
		if (logger->isDebugEnabled()) {
			logger->debug("Connected to (%s)", address.c_str());
		}
	}
	return ret;
}


int MessageClient::send(Message& msg, int timeout) { 
	int ret = net_set_timeout(this->socket, timeout);
	if (ret < 0) return ret;

	if (msg.getId() == "") {
		char uuid[256];
		gen_uuid(uuid, sizeof(uuid));
		msg.setId(uuid);
	}
	ByteBuffer buf;
	msg.encode(buf);
	buf.flip(); 

	int sent = 0, total = buf.remaining();
	unsigned char* start = (unsigned char*)buf.begin();
	while (sent < total) {
		int ret = net_send(this->socket, start, total-sent);
		if (ret < 0) return ret; //error happened
		sent += ret;
		start += ret;
	}
	if (logger->isDebugEnabled()) {
		logger->debug((void*)buf.begin(), buf.remaining());
	}
	return sent;
} 


Message* MessageClient::recv(int& rc, char* msgid, int timeout) {
	if (msgid){
		Message* res = msgTable[msgid];
		msgTable.erase(string(msgid)); 
		if (res) {
			rc = 0;
			return res;
		}
	}

	rc = net_set_timeout(this->socket, timeout);
	if (rc < 0) return NULL;
	
	while (true) {
		unsigned char data[10240];
		int n = net_recv(this->socket, data, sizeof(data));
		if (n < 0) {
			rc = n;
			return NULL;
		} 
		readBuffer->put((void*)data, n);

		ByteBuffer buf(readBuffer); //duplicate, no copy of data
		buf.flip();  
		return 0;
	} 

	return 0;
}