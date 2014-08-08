#ifndef NET_H
#define NET_H

#include "platform.h"

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

#ifdef __cplusplus
extern "C" {
#endif

int  net_connect( int *fd, const char *host, int port );
int  net_bind( int *fd, const char *bind_ip, int port );
int  net_accept( int bind_fd, int *client_fd, void *client_ip );
int  net_set_block( int fd );
int  net_set_nonblock( int fd );
int  net_recv( int fd, unsigned char *buf, size_t len );
int  net_send( int fd, const unsigned char *buf, size_t len );
void net_close( int fd );
int  net_peer_info(int fd, char *ip, int *port);
int  net_set_timeout(int fd, int64_t timeout);


#ifdef __cplusplus
}
#endif

#endif /* net.h */
