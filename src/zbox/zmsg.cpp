#include "include/prelude.h"
#include "include/list.h"
#include "include/zmsg.h"   

#include "../include/zmq.h"

#if ZMQ_VERSION_MAJOR == 2
#   define zmq_sendmsg      zmq_send
#   define zmq_recvmsg      zmq_recv

# 	define zframe_send(msg,s,flags) zmq_send((s),(msg),(flags))
#	define zframe_recv(msg,s,flags) zmq_recv((s),(msg),(flags))
#   define ZMQ_POLL_MSEC    1000        //  zmq_poll is usec
#elif ZMQ_VERSION_MAJOR == 3 || ZMQ_VERSION_MAJOR == 4
#   define ZMQ_POLL_MSEC    1           //  zmq_poll is msec
#	define zframe_send zmq_msg_send
#	define zframe_recv zmq_msg_recv
#endif

struct _zmsg_t {
    list_t* frames;            //  List of frames
    size_t  content_size;      //  Total content size
};

size_t 
zframe_size (zframe_t* self){
	return zmq_msg_size(self);
}

void *
zframe_data (zframe_t* self){
	return zmq_msg_data(self);
}

zframe_t*
zframe_new(const void *data, size_t size){
	zframe_t* frame = (zframe_t*)zmalloc(sizeof(zframe_t));
	assert(frame);

	if(data){
		zmq_msg_init_size(frame, size);
		memcpy(zframe_data(frame), data, size);
	}else{
		zmq_msg_init(frame);
	}

	return frame;
}

zframe_t*
zframe_newstr(const char* string){
	return zframe_new(string ,strlen(string));
}

void
zframe_destroy(zframe_t** self_p){
	assert(self_p);
	zframe_t* self = *self_p;
	if(self){
		zmq_msg_close(self);
		zfree(self);
		*self_p = NULL;
	}
}

char*
zframe_strdup(zframe_t* frame){
    size_t size = zframe_size (frame);
    char *string = (char *) zmalloc (size + 1);
    memcpy (string, zframe_data (frame), size);
    string [size] = 0;
    return string;
}

zframe_t *
zframe_dup (zframe_t *self){
    assert (self);
    return zframe_new (zframe_data (self), zframe_size (self));
}


int
zframe_streq (zframe_t *self, char *string){
    assert (self);
    if (zframe_size (self) == strlen (string)
    		&&  memcmp (zframe_data (self), string, strlen (string)) == 0)
        return 1;
    else
        return 0;
}

char *
zframe_strhex (zframe_t *self){
    static char
        hex_char [] = "0123456789ABCDEF";

    size_t size = zframe_size (self);
    byte *data = (byte*)zframe_data (self);
    char *hex_str = (char *) zmalloc (size * 2 + 1);

    uint byte_nbr;
    for (byte_nbr = 0; byte_nbr < size; byte_nbr++) {
        hex_str [byte_nbr * 2 + 0] = hex_char [data [byte_nbr] >> 4];
        hex_str [byte_nbr * 2 + 1] = hex_char [data [byte_nbr] & 15];
    }
    hex_str [size * 2] = 0;
    return hex_str;
}

void
zframe_log(zframe_t* self){ 
	FILE* file = zlog_get_log_file();  

	byte *data = (byte*)zframe_data (self);
	size_t size = zframe_size (self);
	fprintf (file, "[%03d] ", (int) size);

	char *elipsis = "";
	if(size > 128){
		size = 128;
		elipsis = "...";
	}
	int is_bin = 0;
	uint char_nbr;
	for (char_nbr = 0; char_nbr < size; char_nbr++){
		if (data [char_nbr] < 9 || data [char_nbr] > 127){
			is_bin = 1;
			break;
		}
	}
 
	for (char_nbr = 0; char_nbr < size; char_nbr++) {
		if (is_bin)
			fprintf (file, "%02X", (unsigned char) data [char_nbr]);
		else
			fprintf (file, "%c", data [char_nbr]);
	}
	fprintf (file, "%s\n", elipsis);
}

static void _zframe_destroy(void** ptr){
	zframe_destroy((zframe_t**)ptr);
}


zmsg_t *
zmsg_new (void){
    zmsg_t* self;

    self = (zmsg_t *) zmalloc (sizeof (zmsg_t));
    if (self) {
        self->frames = list_new ();
        self->content_size = 0;
        list_set_destroy(self->frames, _zframe_destroy);
        if (!self->frames) {
            zfree (self);
            return NULL;
        }
    }
    return self;
}



void
zmsg_destroy (zmsg_t** self_p){
    assert (self_p);
    zmsg_t* self = *self_p;
    if(self){
    	list_destroy (&self->frames);
    	zfree (self);
    	*self_p = NULL;
    }
}



size_t
zmsg_frame_size (zmsg_t *self){
    assert (self);
    return list_size (self->frames);
}


size_t
zmsg_content_size (zmsg_t *self){
    assert (self);
    return self->content_size;
}


void
zmsg_push_front (zmsg_t *self, zframe_t *frame){
	assert(frame);
	list_push_front(self->frames, frame);
	self->content_size += zframe_size(frame);
}

void
zmsg_push_back (zmsg_t *self, zframe_t *frame){
	assert(frame);
	list_push_back(self->frames, frame);
	self->content_size += zframe_size(frame);
}

zframe_t *
zmsg_pop_front (zmsg_t *self){
	zframe_t* msg = (zframe_t*)list_pop_front(self->frames);
	if(msg){
		self->content_size -= zframe_size(msg);
	}
	return msg;
}

zframe_t *
zmsg_pop_back (zmsg_t *self){
	zframe_t* msg = (zframe_t*)list_pop_back(self->frames);
	if(msg){
		self->content_size -= zframe_size(msg);
	}
	return msg;
}

void
zmsg_wrap (zmsg_t *self, zframe_t *frame){
	assert( self );
	assert( frame );
	zmsg_push_front(self, zframe_new(NULL,0)); //empty frame
	zmsg_push_front(self, frame);
}

zframe_t*
zmsg_unwrap (zmsg_t *self){
	zframe_t* frame = zmsg_pop_front(self);
	if(!frame) return NULL;

	list_node_t* node = list_head(self->frames);
	if( node &&  zframe_streq((zframe_t*)list_value(node),"") ){
		zframe_t* empty = zmsg_pop_front(self);
		zframe_destroy(&empty);
	}
	return frame;
}


zframe_t *
zmsg_frame (zmsg_t *self, int index){
	list_node_t* node = list_head(self->frames);
	int i = 0;
	while(node && i<index){
		node = list_next(node);
		i++;
	}
	return node? (zframe_t*)list_value(node): NULL;
}

zmsg_t *
zmsg_dup (zmsg_t *self)
{
    assert (self);

    zmsg_t *copy = zmsg_new ();
    if (!copy)
        return NULL;
    list_iter_t* iter = list_iter_new(self->frames, LIST_ITER_FORWARD);
    zframe_t* frame = (zframe_t*)list_iter_next(iter);
    while (frame) {
    	zframe_t* new_frame = zframe_new(zframe_data(frame), zframe_size(frame));
    	if(!new_frame){
    		zmsg_destroy(&copy);
    		return NULL;
    	}
        zmsg_push_back(copy, new_frame);

        frame = (zframe_t*)list_iter_next(iter);
    }
    list_iter_destroy(&iter);
    return copy;
}


void
zmsg_log(zmsg_t* self, char* prefix, ...){ 
	FILE* file = zlog_get_log_file();

	if(!prefix) prefix = "";
	time_t curtime = time (NULL);
	struct tm *loctime = localtime (&curtime);
	char formatted [32];
	strftime (formatted, 32, "%Y-%m-%d %H:%M:%S", loctime);
	fprintf (file, "%s.%03d ", formatted, zclock_time()%1000); 

	va_list argptr;
	va_start (argptr, prefix);
	vfprintf (file, prefix, argptr);
	va_end (argptr); 
	fprintf (file, "\n");

	list_node_t* node = list_head(self->frames);
	while(node){
		zframe_t* frame = (zframe_t*)list_value(node);
		zframe_log(frame);
		node = list_next(node);
	}
	fflush(file); 
}


//  --------------------------------------------------------------------------
//  Send message to socket, destroy after sending. If the message has no
//  frames, sends nothing but destroys the message anyhow.

int
zmsg_send (zmsg_t **self_p, void *socket){
	assert (socket);
	assert(self_p);
	zmsg_t* self = *self_p;
	if(self){
		int count = zmsg_frame_size(self);
		while(count--){
			zframe_t* frame = zmsg_pop_front(self);
			int rc = zframe_send(frame, socket, count>0? ZMQ_SNDMORE : 0);
			if(rc == -1) return -1;
			zframe_destroy(&frame);
		}
		zmsg_destroy(self_p);
	}
    return 0;
}

zmsg_t *
zmsg_recv (void *socket){
	assert(socket);
	zmsg_t* self = zmsg_new();
	if(!self) return NULL;

	int64_t more = 0;
	size_t option_len = sizeof (int64_t);
	while(1){
		zframe_t* frame = zframe_new(NULL,0);
		int rc = zframe_recv(frame, socket, 0);
		if(rc < 0){
			zframe_destroy(&frame);
			zmsg_destroy(&self);
			return NULL;
		}
		zmsg_push_back(self, frame);
		zmq_getsockopt (socket, ZMQ_RCVMORE, &more, &option_len);
		if(!more) break;
	}
	return self;
}

void*
zctx_new(int io_threads){
	assert(io_threads>0); 
	void* ctx = zmq_ctx_new();
	assert(ctx);
	int rc = zmq_ctx_set(ctx, ZMQ_IO_THREADS, 1);
	assert(rc == 0);
	return ctx; 
}


void
zctx_destroy(void** self_p){
	if(!self_p) return;
	void* self = *self_p;
	if(self_p){
		zmq_ctx_destroy(self); 
		*self_p = NULL;
	}
}
