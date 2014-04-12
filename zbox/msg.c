#include "msg.h"   
 
struct frame{
	void* data;
	size_t size; 
};

struct msg {
    list_t* frames;            //  List of frames
    size_t  content_size;      //  Total content size
};

size_t 
frame_size (frame_t* self){
	return self->size;
}

typedef unsigned char   byte; 

void *
frame_data (frame_t* self){
	return self->data;
}

frame_t*
frame_new_nocopy(void* data, size_t size){
	frame_t* self = (frame_t*)malloc(sizeof(frame_t));
	assert(self);

	memset(self, 0, sizeof(frame_t));

	if(data){
		self->data = data; 
		self->size = size; 
	}

	return self;
}

frame_t*
frame_new(const void *data, size_t size){
	frame_t* self = (frame_t*)malloc(sizeof(frame_t));
	assert(self);
	
	memset(self, 0, sizeof(frame_t));
	
	if(data){
		self->data = malloc(size); 
		self->size = size;
		memcpy(self->data, data, size);
	}

	return self;
}

frame_t*
frame_newstr(const char* string){
	return frame_new(string ,strlen(string));
}

void
frame_destroy(frame_t** self_p){
	frame_t* self;
	assert(self_p);
	self = *self_p;
	if(self){
		if(self->data) free(self->data);
		free(self); 
		*self_p = NULL;
	}
}

char*
frame_strdup(frame_t* frame){
    size_t size = frame_size (frame);
    char *string = (char *) malloc (size + 1);
    memcpy (string, frame_data (frame), size);
    string [size] = 0;
    return string;
}

frame_t *
frame_dup (frame_t *self){
    assert (self);
    return frame_new (frame_data (self), frame_size (self));
}


int
frame_streq (frame_t *self, char *string){
    assert (self);
    if (frame_size (self) == strlen (string)
    		&&  memcmp (frame_data (self), string, strlen (string)) == 0)
        return 1;
    else
        return 0;
}

char *
frame_strhex (frame_t *self){
    static char
        hex_char [] = "0123456789ABCDEF";

    size_t size = frame_size (self);
    byte *data = (byte*)frame_data (self);
    char *hex_str = (char *) malloc (size * 2 + 1);

    size_t byte_nbr;

    for (byte_nbr = 0; byte_nbr < size; byte_nbr++) {
        hex_str [byte_nbr * 2 + 0] = hex_char [data [byte_nbr] >> 4];
        hex_str [byte_nbr * 2 + 1] = hex_char [data [byte_nbr] & 15];
    }
    hex_str [size * 2] = 0;
    return hex_str;
}


static void _zframe_destroy(void** ptr){
	frame_destroy((frame_t**)ptr);
}


msg_t *
msg_new (void){
    msg_t* self;

    self = (msg_t *) malloc (sizeof (msg_t));
    if (self) {
        self->frames = list_new ();
        self->content_size = 0;
        list_set_destroy(self->frames, _zframe_destroy);
        if (!self->frames) {
            free (self);
            return NULL;
        }
    }
    return self;
}



void
msg_destroy (msg_t** self_p){
	msg_t* self;
    assert (self_p);
    self = *self_p;
    if(self){
    	list_destroy (&self->frames);
    	free (self);
    	*self_p = NULL;
    }
}



size_t
msg_frame_size (msg_t *self){
    assert (self);
    return list_size (self->frames);
}


size_t
msg_content_size (msg_t *self){
    assert (self);
    return self->content_size;
}

list_t*
msg_frames (msg_t *self){
	return self->frames;
}
void
msg_push_front (msg_t *self, frame_t *frame){
	assert(frame);
	list_push_front(self->frames, frame);
	self->content_size += frame_size(frame);
}

void msg_push_frontstr(msg_t* self, char* string){
	msg_push_front(self, frame_newstr(string));
}

void
msg_push_back (msg_t *self, frame_t *frame){
	assert(frame);
	list_push_back(self->frames, frame);
	self->content_size += frame_size(frame);
}
void
msg_push_backstr(msg_t* self, char* string){
	msg_push_back(self, frame_newstr(string));
}



frame_t *
msg_pop_front (msg_t *self){
	frame_t* msg = (frame_t*)list_pop_front(self->frames);
	if(msg){
		self->content_size -= frame_size(msg);
	}
	return msg;
}

frame_t *
msg_pop_back (msg_t *self){
	frame_t* msg = (frame_t*)list_pop_back(self->frames);
	if(msg){
		self->content_size -= frame_size(msg);
	}
	return msg;
}


frame_t *
zmsg_frame (msg_t *self, int index){
	list_node_t* node = list_head(self->frames);
	int i = 0;
	while(node && i<index){
		node = list_next(node);
		i++;
	}
	return node? (frame_t*)list_value(node): NULL;
}

msg_t *
msg_dup (msg_t *self){
	msg_t *copy;
	list_node_t* node; 
    assert (self);

    copy = msg_new ();
    if (!copy) return NULL; 

    node = list_head(self->frames);
    while (node) {
		frame_t* frame = list_value(node);
    	frame_t* new_frame = frame_new(frame_data(frame), frame_size(frame));
    	if(!new_frame){
    		msg_destroy(&copy);
    		return NULL;
    	}
        msg_push_back(copy, new_frame);

        node =list_next(node);
    } 
    return copy;
}
void
frame_print (frame_t *self, FILE* file){
	const char* fmt = "[%04d] %s\n";
	const size_t MAX_SIZE = 10240;
	size_t len = frame_size(self);
	if(len>MAX_SIZE){
		fprintf(file, fmt, len, "{LARGE BLOB}");
	} else {
		char* data = frame_strdup(self);
		fprintf(file, fmt, len, data);
		free(data);
	}
}
void
msg_print (msg_t *self, FILE* file){
	list_node_t* node = list_head(self->frames);
	while(node){
		frame_t* frame = list_value(node);
		frame_print(frame, file);
		node = list_next(node);
	}
}
/*
int main2(int argc, char* argv[]){
	zframe_t* frame;
	zmsg_t* msg;
	char* string;
	list_node_t* node;

	msg = zmsg_new();
	frame = zframe_newstr("myframe");
	
	zmsg_push_back(msg, frame);
	zmsg_push_back(msg, zframe_newstr("hello"));

	node = list_find(msg->frames, frame);
	printf("%d\n", node);
	string = zframe_strdup(frame);
	printf("%s\n", string);

	free(string); 
	zmsg_destroy(&msg);

	frame = zframe_new_nocopy("test", strlen("test"));
	zframe_destroy(&frame);

	getch();

	return 0;
}
*/