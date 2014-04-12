#include "net.h"

#ifdef __WINDOWS__

#define read(fd,buf,len)        recv(fd,(char*)buf,(int) len,0)
#define write(fd,buf,len)       send(fd,(char*)buf,(int) len,0)
#define close(fd)               closesocket(fd)

static int wsa_init_done = 0;
#endif
 

#define POLARSSL_HTONS(n) ((((unsigned short)(n) & 0xFF      ) << 8 ) | \
                           (((unsigned short)(n) & 0xFF00    ) >> 8 ))
#define POLARSSL_HTONL(n) ((((unsigned long )(n) & 0xFF      ) << 24) | \
                           (((unsigned long )(n) & 0xFF00    ) << 8 ) | \
                           (((unsigned long )(n) & 0xFF0000  ) >> 8 ) | \
                           (((unsigned long )(n) & 0xFF000000) >> 24))

unsigned short net_htons(unsigned short n);
unsigned long  net_htonl(unsigned long  n);
#define net_htons(n) POLARSSL_HTONS(n)
#define net_htonl(n) POLARSSL_HTONL(n)


/*
 * Prepare for using the sockets interface
 */
static int net_prepare( void )
{
#ifdef __WINDOWS__ 
    if( wsa_init_done == 0 )
    {
		WSADATA wsaData;
        if( WSAStartup( MAKEWORD(2,0), &wsaData ) == SOCKET_ERROR )
            return( ERR_NET_SOCKET_FAILED );

        wsa_init_done = 1;
    }
#else

#if !defined(EFIX64) && !defined(EFI32)
    signal( SIGPIPE, SIG_IGN );
#endif

#endif
    return( 0 );
}

/*
 * Initiate a TCP connection with host:port
 */
int net_connect( int *fd, const char *host, int port )
{ 
    int ret;
    struct addrinfo hints, *addr_list, *cur;
    char port_str[6];

    if( ( ret = net_prepare() ) != 0 )
        return( ret );

    /* getaddrinfo expects port as a string */
    memset( port_str, 0, sizeof( port_str ) );
    snprintf( port_str, sizeof( port_str ), "%d", port );

    /* Do name resolution with both IPv6 and IPv4, but only TCP */
    memset( &hints, 0, sizeof( hints ) );
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    if( getaddrinfo( host, port_str, &hints, &addr_list ) != 0 )
        return( ERR_NET_UNKNOWN_HOST );

    /* Try the sockaddrs until a connection succeeds */
    ret = ERR_NET_UNKNOWN_HOST;
    for( cur = addr_list; cur != NULL; cur = cur->ai_next )
    {
        *fd = (int) socket( cur->ai_family, cur->ai_socktype,
                            cur->ai_protocol );
        if( *fd < 0 )
        {
            ret = ERR_NET_SOCKET_FAILED;
            continue;
        }

        if( connect( *fd, cur->ai_addr, cur->ai_addrlen ) == 0 )
        {
            ret = 0;
            break;
        }

        close( *fd );
        ret = ERR_NET_CONNECT_FAILED;
    }

    freeaddrinfo( addr_list );

    return( ret ); 
}

/*
 * Create a listening socket on bind_ip:port
 */
int net_bind( int *fd, const char *bind_ip, int port )
{ 
    int n, ret;
    struct addrinfo hints, *addr_list, *cur;
    char port_str[6];

    if( ( ret = net_prepare() ) != 0 )
        return( ret );

    /* getaddrinfo expects port as a string */
    memset( port_str, 0, sizeof( port_str ) );
    snprintf( port_str, sizeof( port_str ), "%d", port );

    /* Bind to IPv6 and/or IPv4, but only in TCP */
    memset( &hints, 0, sizeof( hints ) );
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;
    if( bind_ip == NULL )
        hints.ai_flags = AI_PASSIVE;

    if( getaddrinfo( bind_ip, port_str, &hints, &addr_list ) != 0 )
        return( ERR_NET_UNKNOWN_HOST );

    /* Try the sockaddrs until a binding succeeds */
    ret = ERR_NET_UNKNOWN_HOST;
    for( cur = addr_list; cur != NULL; cur = cur->ai_next )
    {
        *fd = (int) socket( cur->ai_family, cur->ai_socktype,
                            cur->ai_protocol );
        if( *fd < 0 )
        {
            ret = ERR_NET_SOCKET_FAILED;
            continue;
        }

        n = 1;
        setsockopt( *fd, SOL_SOCKET, SO_REUSEADDR,
                    (const char *) &n, sizeof( n ) );

        if( bind( *fd, cur->ai_addr, cur->ai_addrlen ) != 0 )
        {
            close( *fd );
            ret = ERR_NET_BIND_FAILED;
            continue;
        }

        if( listen( *fd, NET_LISTEN_BACKLOG ) != 0 )
        {
            close( *fd );
            ret = ERR_NET_LISTEN_FAILED;
            continue;
        }

        /* I we ever get there, it's a success */
        ret = 0;
        break;
    }

    freeaddrinfo( addr_list );

    return( ret );
}

#ifdef __WINDOWS__
/*
 * Check if the requested operation would be blocking on a non-blocking socket
 * and thus 'failed' with a negative return value.
 */
static int net_would_block( int fd )
{
    return( WSAGetLastError() == WSAEWOULDBLOCK );
}
#else
/*
 * Check if the requested operation would be blocking on a non-blocking socket
 * and thus 'failed' with a negative return value.
 *
 * Note: on a blocking socket this function always returns 0!
 */
static int net_would_block( int fd )
{
    /*
     * Never return 'WOULD BLOCK' on a non-blocking socket
     */
    if( ( fcntl( fd, F_GETFL ) & O_NONBLOCK ) != O_NONBLOCK )
        return( 0 );

    switch( errno )
    {
#if defined EAGAIN
        case EAGAIN:
#endif
#if defined EWOULDBLOCK && EWOULDBLOCK != EAGAIN
        case EWOULDBLOCK:
#endif
            return( 1 );
    }
    return( 0 );
}
#endif

/*
 * Accept a connection from a remote client
 */
int net_accept( int bind_fd, int *client_fd, void *client_ip )
{
	struct sockaddr_storage client_addr; 
#ifdef __WINDOWS__
    socklen_t n = (socklen_t) sizeof( client_addr );
#else
    int n = (int) sizeof( client_addr );
#endif

    *client_fd = (int) accept( bind_fd, (struct sockaddr *)
                               &client_addr, &n );

    if( *client_fd < 0 )
    {
        if( net_would_block( *client_fd ) != 0 )
            return( ERR_NET_WANT_READ );

        return( ERR_NET_ACCEPT_FAILED );
    }

    if( client_ip != NULL ) { 
        if( client_addr.ss_family == AF_INET ) {
            struct sockaddr_in *addr4 = (struct sockaddr_in *) &client_addr;
            memcpy( client_ip, &addr4->sin_addr.s_addr,
                        sizeof( addr4->sin_addr.s_addr ) );
        }  else {
            struct sockaddr_in6 *addr6 = (struct sockaddr_in6 *) &client_addr;
            memcpy( client_ip, &addr6->sin6_addr.s6_addr,
                        sizeof( addr6->sin6_addr.s6_addr ) );
        } 
    }

    return( 0 );
}

/*
 * Set the socket blocking or non-blocking
 */
int net_set_block( int fd )
{
#ifdef __WINDOWS__
    u_long n = 0;
    return( ioctlsocket( fd, FIONBIO, &n ) );
#else
    return( fcntl( fd, F_SETFL, fcntl( fd, F_GETFL ) & ~O_NONBLOCK ) );
#endif
}

int net_set_nonblock( int fd )
{
#ifdef __WINDOWS__
    u_long n = 1;
    return( ioctlsocket( fd, FIONBIO, &n ) );
#else
    return( fcntl( fd, F_SETFL, fcntl( fd, F_GETFL ) | O_NONBLOCK ) );
#endif
}
 

/*
 * Read at most 'len' characters
 */
int net_recv( int fd, unsigned char *buf, size_t len )
{ 
    int ret = read( fd, buf, len );

    if( ret < 0 )
    {
        if( net_would_block( fd ) != 0 )
            return( ERR_NET_WANT_READ );

#ifdef __WINDOWS__
        if( WSAGetLastError() == WSAECONNRESET )
            return( ERR_NET_CONN_RESET );
#else
        if( errno == EPIPE || errno == ECONNRESET )
            return( ERR_NET_CONN_RESET );

        if( errno == EINTR )
            return( ERR_NET_WANT_READ );
#endif

        return( ERR_NET_RECV_FAILED );
    }

    return( ret );
}

/*
 * Write at most 'len' characters
 */
int net_send( int fd, const unsigned char *buf, size_t len )
{ 
    int ret = write( fd, buf, len );

    if( ret < 0 )
    {
        if( net_would_block( fd ) != 0 )
            return( ERR_NET_WANT_WRITE );

#ifdef __WINDOWS__
        if( WSAGetLastError() == WSAECONNRESET )
            return( ERR_NET_CONN_RESET );
#else
        if( errno == EPIPE || errno == ECONNRESET )
            return( ERR_NET_CONN_RESET );

        if( errno == EINTR )
            return( ERR_NET_WANT_WRITE );
#endif

        return( ERR_NET_SEND_FAILED );
    }

    return( ret );
}

/*
 * Gracefully close the connection
 */
void net_close( int fd )
{
    shutdown( fd, 2 );
    close( fd );
}
 

int net_peer_info(int fd, char *ip, int *port) {
	struct sockaddr_in sa;
	socklen_t salen = sizeof(sa);

	if (getpeername(fd,(struct sockaddr*)&sa,&salen) == -1) {
		*port = 0;
		ip[0] = '?';
		ip[1] = '\0';
		return -1;
	}
	if (ip) strcpy(ip,inet_ntoa(sa.sin_addr));
	if (port) *port = ntohs(sa.sin_port);
	return 0;
}