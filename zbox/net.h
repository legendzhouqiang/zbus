#ifndef NET_H
#define NET_H

#include "platform.h"

#define ERR_NET_UNKNOWN_HOST                      -0x0056  /**< Failed to get an IP address for the given hostname. */
#define ERR_NET_SOCKET_FAILED                     -0x0042  /**< Failed to open a socket. */
#define ERR_NET_CONNECT_FAILED                    -0x0044  /**< The connection to the given server / port failed. */
#define ERR_NET_BIND_FAILED                       -0x0046  /**< Binding of the socket failed. */
#define ERR_NET_LISTEN_FAILED                     -0x0048  /**< Could not listen on the socket. */
#define ERR_NET_ACCEPT_FAILED                     -0x004A  /**< Could not accept the incoming connection. */
#define ERR_NET_RECV_FAILED                       -0x004C  /**< Reading information from the socket failed. */
#define ERR_NET_SEND_FAILED                       -0x004E  /**< Sending information through the socket failed. */
#define ERR_NET_CONN_RESET                        -0x0050  /**< Connection was reset by peer. */
#define ERR_NET_WANT_READ                         -0x0052  /**< Connection requires a read call. */
#define ERR_NET_WANT_WRITE                        -0x0054  /**< Connection requires a write call. */

#define NET_LISTEN_BACKLOG         10 /**< The backlog that listen() should use. */

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

#ifdef __cplusplus
}
#endif

#endif /* net.h */
