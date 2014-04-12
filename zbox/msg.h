#ifndef __ZBOX_MSG_H_
#define __ZBOX_MSG_H_

#include "platform.h"
#include "list.h"

#ifdef __cplusplus
extern "C" {
#endif


typedef struct frame frame_t;
typedef struct msg msg_t;

ZBOX_EXPORT size_t 
	frame_size (frame_t* self);

ZBOX_EXPORT void *
	frame_data (frame_t* self);


ZBOX_EXPORT frame_t*
	frame_new (const void *src, size_t size);
ZBOX_EXPORT frame_t*
	frame_newstr(const char* string);
ZBOX_EXPORT frame_t*
	frame_new_nocopy(void* data, size_t size);

ZBOX_EXPORT void
	frame_destroy (frame_t** self_p);
ZBOX_EXPORT char*
	frame_strdup (frame_t* frame);
ZBOX_EXPORT frame_t *
	frame_dup (frame_t *self);
ZBOX_EXPORT int
	frame_streq (frame_t *self, char *string);
ZBOX_EXPORT char *
	frame_strhex (frame_t *self); 
ZBOX_EXPORT void
	frame_print (frame_t *self, FILE* file);

//- zmsg
ZBOX_EXPORT msg_t *
    msg_new (void);
ZBOX_EXPORT msg_t *
    msg_dup (msg_t *self);
ZBOX_EXPORT void
    msg_destroy (msg_t **self_p);

//  Return frames of the msg
ZBOX_EXPORT list_t*
	msg_frames (msg_t *self);

//  Return number of frames in message
ZBOX_EXPORT size_t
    msg_frame_size (msg_t *self);

//  Return combined size of all frames in message
ZBOX_EXPORT size_t
    msg_content_size (msg_t *self);


//  Push frame to front of message, before first frame
ZBOX_EXPORT void
	msg_push_front (msg_t *self, frame_t *frame);
ZBOX_EXPORT void
	msg_push_frontstr(msg_t* self, char* string);

//  Add frame to end of message, after last frame
ZBOX_EXPORT void
	msg_push_back (msg_t *self, frame_t *frame);
ZBOX_EXPORT void
	msg_push_backstr(msg_t* self, char* string);

//  Pop frame off front of message, caller now owns frame
ZBOX_EXPORT frame_t *
	msg_pop_front (msg_t *self);

//  Pop frame off tail of message, caller now owns frame
ZBOX_EXPORT frame_t *
    msg_pop_back (msg_t *self);




ZBOX_EXPORT void
	msg_print (msg_t *self, FILE* file);


#ifdef __cplusplus
}
#endif

#endif
