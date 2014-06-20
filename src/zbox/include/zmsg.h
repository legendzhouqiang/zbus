#ifndef __ZBOX_ZMSG_H_INCLUDED__
#define __ZBOX_ZMSG_H_INCLUDED__ 

#ifdef __cplusplus
extern "C" {
#endif
 
#if (defined (__WINDOWS__))
#   if defined DLL_EXPORT
#       define ZBOX_EXPORT __declspec(dllexport)
#   else
#       define ZBOX_EXPORT __declspec(dllimport)
#   endif
#else
#   define ZBOX_EXPORT
#endif


typedef struct _zmsg_t zmsg_t;
typedef struct zmq_msg_t zframe_t;

ZBOX_EXPORT size_t 
	zframe_size (zframe_t* self);

ZBOX_EXPORT void *
	zframe_data (zframe_t* self);

ZBOX_EXPORT void*
	zctx_new(int io_threads);
ZBOX_EXPORT void
	zctx_destroy(void** self_p);


ZBOX_EXPORT zframe_t*
	zframe_new (const void *src, size_t size);
ZBOX_EXPORT zframe_t*
	zframe_newstr(const char* string);
ZBOX_EXPORT void
	zframe_destroy (zframe_t** self_p);
ZBOX_EXPORT char*
	zframe_strdup (zframe_t* frame);
ZBOX_EXPORT zframe_t *
	zframe_dup (zframe_t *self);
ZBOX_EXPORT int
	zframe_streq (zframe_t *self, char *string);
ZBOX_EXPORT char *
	zframe_strhex (zframe_t *self); 
ZBOX_EXPORT void
	zframe_log (zframe_t *self);

//- zmsg
ZBOX_EXPORT zmsg_t *
    zmsg_new (void);
ZBOX_EXPORT zmsg_t *
    zmsg_dup (zmsg_t *self);
ZBOX_EXPORT void
    zmsg_destroy (zmsg_t **self_p);

ZBOX_EXPORT zmsg_t *
    zmsg_recv (void *socket);
ZBOX_EXPORT int
    zmsg_send (zmsg_t **self_p, void *socket);

//  Return number of frames in message
ZBOX_EXPORT size_t
    zmsg_frame_size (zmsg_t *self);

//  Return combined size of all frames in message
ZBOX_EXPORT size_t
    zmsg_content_size (zmsg_t *self);


//  Push frame to front of message, before first frame
ZBOX_EXPORT void
	zmsg_push_front (zmsg_t *self, zframe_t *frame);

//  Add frame to end of message, after last frame
ZBOX_EXPORT void
	zmsg_push_back (zmsg_t *self, zframe_t *frame);

//  Pop frame off front of message, caller now owns frame
ZBOX_EXPORT zframe_t *
	zmsg_pop_front (zmsg_t *self);

//  Pop frame off tail of message, caller now owns frame
ZBOX_EXPORT zframe_t *
    zmsg_pop_back (zmsg_t *self);



//  Push frame to front of message, before first frame
//  Pushes an empty frame in front of frame
ZBOX_EXPORT void
    zmsg_wrap (zmsg_t *self, zframe_t *frame);

//  Pop frame off front of message, caller now owns frame
//  If next frame is empty, pops and destroys that empty frame.
ZBOX_EXPORT zframe_t *
    zmsg_unwrap (zmsg_t *self);


ZBOX_EXPORT zframe_t*
	zmsg_frame(zmsg_t* self, int index); 

ZBOX_EXPORT void
	zmsg_log(zmsg_t* self, char* prefix, ...);


#ifdef __cplusplus
}
#endif

#endif
