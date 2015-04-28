#include "json.h"

//============================================================================
static const char *ep;
const char *json_error(void) {return ep;}
static int cJSON_strcasecmp(const char *s1,const char *s2)
{
	if (!s1) return (s1==s2)?0:1;if (!s2) return 1;
	for(; tolower(*s1) == tolower(*s2); ++s1, ++s2)	if(*s1 == 0)	return 0;
	return tolower(*(const unsigned char *)s1) - tolower(*(const unsigned char *)s2);
}

/* Internal constructor. */
static json_t *cJSON_New_Item(void)
{
	json_t* node = (json_t*)malloc(sizeof(json_t));
	if (node) memset(node,0,sizeof(json_t));
	return node;
}

/* Delete a cJSON structure. */
void json_destroy(json_t *c)
{
	json_t *next;
	while (c)
	{
		next=c->next;
		if (!(c->type&JSON_IsReference) && c->child) json_destroy(c->child);
		if (!(c->type&JSON_IsReference) && c->valuestring) free(c->valuestring);
		if (c->string) free(c->string);
		free(c);
		c=next;
	}
}

/* Parse the input text to generate a number, and populate the result into item. */
static const char *parse_number(json_t *item,const char *num)
{
	double n=0,sign=1,scale=0;int subscale=0,signsubscale=1;

	/* Could use sscanf for this? */
	if (*num=='-') sign=-1,num++;	/* Has sign? */
	if (*num=='0') num++;			/* is zero */
	if (*num>='1' && *num<='9')	do	n=(n*10.0)+(*num++ -'0');	while (*num>='0' && *num<='9');	/* Number? */
	if (*num=='.' && num[1]>='0' && num[1]<='9') {num++;		do	n=(n*10.0)+(*num++ -'0'),scale--; while (*num>='0' && *num<='9');}	/* Fractional part? */
	if (*num=='e' || *num=='E')		/* Exponent? */
	{	num++;if (*num=='+') num++;	else if (*num=='-') signsubscale=-1,num++;		/* With sign? */
	while (*num>='0' && *num<='9') subscale=(subscale*10)+(*num++ - '0');	/* Number? */
	}

	n=sign*n*pow(10.0,(scale+subscale*signsubscale));	/* number = +/- number.fraction * 10^+/- exponent */

	item->valuedouble=n;
	item->valueint=(int)n;
	item->type=JSON_NUMBER;
	return num;
}

/* Render the number nicely from the given item into a string. */
static char *print_number(json_t *item)
{
	char *str;
	double d=item->valuedouble;
	if (fabs(((double)item->valueint)-d)<=DBL_EPSILON && d<=INT_MAX && d>=INT_MIN)
	{
		str=(char*)malloc(21);	/* 2^64+1 can be represented in 21 chars. */
		if (str) sprintf(str,"%d",item->valueint);
	}
	else
	{
		str=(char*)malloc(64);	/* This is a nice tradeoff. */
		if (str)
		{
			if (fabs(floor(d)-d)<=DBL_EPSILON && fabs(d)<1.0e60)sprintf(str,"%.0f",d);
			else if (fabs(d)<1.0e-6 || fabs(d)>1.0e9)			sprintf(str,"%e",d);
			else												sprintf(str,"%f",d);
		}
	}
	return str;
}

/* Parse the input text into an unescaped cstring, and populate item. */
static const unsigned char firstByteMark[7] = { 0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC };
static const char *parse_string(json_t *item,const char *str)
{
	const char *ptr=str+1;char *ptr2;char *out;int len=0;unsigned uc,uc2;
	if (*str!='\"') {ep=str;return 0;}	/* not a string! */

	while (*ptr!='\"' && *ptr && ++len) if (*ptr++ == '\\') ptr++;	/* Skip escaped quotes. */

	out=(char*)malloc(len+1);	/* This is how long we need for the string, roughly. */
	if (!out) return 0;

	ptr=str+1;ptr2=out;
	while (*ptr!='\"' && *ptr)
	{
		if (*ptr!='\\') *ptr2++=*ptr++;
		else
		{
			ptr++;
			switch (*ptr)
			{
			case 'b': *ptr2++='\b';	break;
			case 'f': *ptr2++='\f';	break;
			case 'n': *ptr2++='\n';	break;
			case 'r': *ptr2++='\r';	break;
			case 't': *ptr2++='\t';	break;
			case 'u':	 /* transcode utf16 to utf8. */
				sscanf(ptr+1,"%4x",&uc);ptr+=4;	/* get the unicode char. */

				if ((uc>=0xDC00 && uc<=0xDFFF) || uc==0)	break;	/* check for invalid.	*/

				if (uc>=0xD800 && uc<=0xDBFF)	/* UTF16 surrogate pairs.	*/
				{
					if (ptr[1]!='\\' || ptr[2]!='u')	break;	/* missing second-half of surrogate.	*/
					sscanf(ptr+3,"%4x",&uc2);ptr+=6;
					if (uc2<0xDC00 || uc2>0xDFFF)		break;	/* invalid second-half of surrogate.	*/
					uc=0x10000 + (((uc&0x3FF)<<10) | (uc2&0x3FF));
				}

				len=4;if (uc<0x80) len=1;else if (uc<0x800) len=2;else if (uc<0x10000) len=3; ptr2+=len;

				switch (len) {
			case 4: *--ptr2 =((uc | 0x80) & 0xBF); uc >>= 6;
			case 3: *--ptr2 =((uc | 0x80) & 0xBF); uc >>= 6;
			case 2: *--ptr2 =((uc | 0x80) & 0xBF); uc >>= 6;
			case 1: *--ptr2 =(uc | firstByteMark[len]);
				}
				ptr2+=len;
				break;
			default:  *ptr2++=*ptr; break;
			}
			ptr++;
		}
	}
	*ptr2=0;
	if (*ptr=='\"') ptr++;
	item->valuestring=out;
	item->type=JSON_STRING;
	return ptr;
}

/* Render the cstring provided to an escaped version that can be printed. */
static char *print_string_ptr(const char *str)
{
	const char *ptr;char *ptr2,*out;int len=0;unsigned char token;

	if (!str) return strdup("");
	ptr=str;while ((token=*ptr) && ++len) {if (strchr("\"\\\b\f\n\r\t",token)) len++; else if (token<32) len+=5;ptr++;}

	out=(char*)malloc(len+3);
	if (!out) return 0;

	ptr2=out;ptr=str;
	*ptr2++='\"';
	while (*ptr)
	{
		if ((unsigned char)*ptr>31 && *ptr!='\"' && *ptr!='\\') *ptr2++=*ptr++;
		else
		{
			*ptr2++='\\';
			switch (token=*ptr++)
			{
			case '\\':	*ptr2++='\\';	break;
			case '\"':	*ptr2++='\"';	break;
			case '\b':	*ptr2++='b';	break;
			case '\f':	*ptr2++='f';	break;
			case '\n':	*ptr2++='n';	break;
			case '\r':	*ptr2++='r';	break;
			case '\t':	*ptr2++='t';	break;
			default: sprintf(ptr2,"u%04x",token);ptr2+=5;	break;	/* escape and print */
			}
		}
	}
	*ptr2++='\"';*ptr2++=0;
	return out;
}
/* Invote print_string_ptr (which is useful) on an item. */
static char *print_string(json_t *item)	{return print_string_ptr(item->valuestring);}

/* Predeclare these prototypes. */
static const char *parse_value(json_t *item,const char *value);
static char *print_value(json_t *item,int depth,int fmt);
static const char *parse_array(json_t *item,const char *value);
static char *print_array(json_t *item,int depth,int fmt);
static const char *parse_object(json_t *item,const char *value);
static char *print_object(json_t *item,int depth,int fmt);

/* Utility to jump whitespace and cr/lf */
static const char *skip(const char *in) {while (in && *in && (unsigned char)*in<=32) in++; return in;}

/* Parse an object - create a new root, and populate. */
json_t *json_parse_ext(const char *value,const char **return_parse_end,int require_null_terminated)
{
	const char *end=0;
	json_t *c=cJSON_New_Item();
	ep=0;
	if (!c) return 0;       /* memory fail */

	end=parse_value(c,skip(value));
	if (!end)	{json_destroy(c);return 0;}	/* parse failure. ep is set. */

	/* if we require null-terminated JSON without appended garbage, skip and then check for a null terminator */
	if (require_null_terminated) {end=skip(end);if (*end) {json_destroy(c);ep=end;return 0;}}
	if (return_parse_end) *return_parse_end=end;
	return c;
}
/* Default options for cJSON_Parse */
json_t *json_parse(const char *value) {return json_parse_ext(value,0,0);}

/* Render a cJSON item/entity/structure to text. */
char *json_dump(json_t *item)				{return print_value(item,0,1);}
char *json_dump_raw(json_t *item)	{return print_value(item,0,0);}

/* Parser core - when encountering text, process appropriately. */
static const char *parse_value(json_t *item,const char *value)
{
	if (!value)						return 0;	/* Fail on null. */
	if (!strncmp(value,"null",4))	{ item->type=JSON_NULL;  return value+4; }
	if (!strncmp(value,"false",5))	{ item->type=JSON_FALSE; return value+5; }
	if (!strncmp(value,"true",4))	{ item->type=JSON_TRUE; item->valueint=1;	return value+4; }
	if (*value=='\"')				{ return parse_string(item,value); }
	if (*value=='-' || (*value>='0' && *value<='9'))	{ return parse_number(item,value); }
	if (*value=='[')				{ return parse_array(item,value); }
	if (*value=='{')				{ return parse_object(item,value); }

	ep=value;return 0;	/* failure. */
}

/* Render a value to text. */
char *print_value(json_t *item,int depth,int fmt)
{
	char *out=0;
	if (!item) return 0;
	switch ((item->type)&255)
	{
	case JSON_NULL:	out=strdup("null");	break;
	case JSON_FALSE:	out=strdup("false");break;
	case JSON_TRUE:	out=strdup("true"); break;
	case JSON_NUMBER:	out=print_number(item);break;
	case JSON_STRING:	out=print_string(item);break;
	case JSON_ARRAY:	out=print_array(item,depth,fmt);break;
	case JSON_OBJECT:	out=print_object(item,depth,fmt);break;
	}
	return out;
}

/* Build an array from input text. */
static const char *parse_array(json_t *item,const char *value)
{
	json_t *child;
	if (*value!='[')	{ep=value;return 0;}	/* not an array! */

	item->type=JSON_ARRAY;
	value=skip(value+1);
	if (*value==']') return value+1;	/* empty array. */

	item->child=child=cJSON_New_Item();
	if (!item->child) return 0;		 /* memory fail */
	value=skip(parse_value(child,skip(value)));	/* skip any spacing, get the value. */
	if (!value) return 0;

	while (*value==',')
	{
		json_t *new_item;
		if (!(new_item=cJSON_New_Item())) return 0; 	/* memory fail */
		child->next=new_item;new_item->prev=child;child=new_item;
		value=skip(parse_value(child,skip(value+1)));
		if (!value) return 0;	/* memory fail */
	}

	if (*value==']') return value+1;	/* end of array */
	ep=value;return 0;	/* malformed. */
}

/* Render an array to text */
static char *print_array(json_t *item,int depth,int fmt)
{
	char **entries;
	char *out=0,*ptr,*ret;int len=5;
	json_t *child=item->child;
	int numentries=0,i=0,fail=0;

	/* How many entries in the array? */
	while (child) numentries++,child=child->next;
	/* Explicitly handle numentries==0 */
	if (!numentries)
	{
		out=(char*)malloc(3);
		if (out) strcpy(out,"[]");
		return out;
	}
	/* Allocate an array to hold the values for each */
	entries=(char**)malloc(numentries*sizeof(char*));
	if (!entries) return 0;
	memset(entries,0,numentries*sizeof(char*));
	/* Retrieve all the results: */
	child=item->child;
	while (child && !fail)
	{
		ret=print_value(child,depth+1,fmt);
		entries[i++]=ret;
		if (ret) len+=strlen(ret)+2+(fmt?1:0); else fail=1;
		child=child->next;
	}

	/* If we didn't fail, try to malloc the output string */
	if (!fail) out=(char*)malloc(len);
	/* If that fails, we fail. */
	if (!out) fail=1;

	/* Handle failure. */
	if (fail)
	{
		for (i=0;i<numentries;i++) if (entries[i]) free(entries[i]);
		free(entries);
		return 0;
	}

	/* Compose the output array. */
	*out='[';
	ptr=out+1;*ptr=0;
	for (i=0;i<numentries;i++)
	{
		strcpy(ptr,entries[i]);ptr+=strlen(entries[i]);
		if (i!=numentries-1) {*ptr++=',';if(fmt)*ptr++=' ';*ptr=0;}
		free(entries[i]);
	}
	free(entries);
	*ptr++=']';*ptr++=0;
	return out;	
}

/* Build an object from the text. */
static const char *parse_object(json_t *item,const char *value)
{
	json_t *child;
	if (*value!='{')	{ep=value;return 0;}	/* not an object! */

	item->type=JSON_OBJECT;
	value=skip(value+1);
	if (*value=='}') return value+1;	/* empty array. */

	item->child=child=cJSON_New_Item();
	if (!item->child) return 0;
	value=skip(parse_string(child,skip(value)));
	if (!value) return 0;
	child->string=child->valuestring;child->valuestring=0;
	if (*value!=':') {ep=value;return 0;}	/* fail! */
	value=skip(parse_value(child,skip(value+1)));	/* skip any spacing, get the value. */
	if (!value) return 0;

	while (*value==',')
	{
		json_t *new_item;
		if (!(new_item=cJSON_New_Item()))	return 0; /* memory fail */
		child->next=new_item;new_item->prev=child;child=new_item;
		value=skip(parse_string(child,skip(value+1)));
		if (!value) return 0;
		child->string=child->valuestring;child->valuestring=0;
		if (*value!=':') {ep=value;return 0;}	/* fail! */
		value=skip(parse_value(child,skip(value+1)));	/* skip any spacing, get the value. */
		if (!value) return 0;
	}

	if (*value=='}') return value+1;	/* end of array */
	ep=value;return 0;	/* malformed. */
}

/* Render an object to text. */
static char *print_object(json_t *item,int depth,int fmt)
{
	char **entries=0,**names=0;
	char *out=0,*ptr,*ret,*str;int len=7,i=0,j;
	json_t *child=item->child;
	int numentries=0,fail=0;
	/* Count the number of entries. */
	while (child) numentries++,child=child->next;
	/* Explicitly handle empty object case */
	if (!numentries)
	{
		out=(char*)malloc(fmt?depth+3:3);
		if (!out)	return 0;
		ptr=out;*ptr++='{';
		if (fmt) {*ptr++='\n';for (i=0;i<depth-1;i++) *ptr++='\t';}
		*ptr++='}';*ptr++=0;
		return out;
	}
	/* Allocate space for the names and the objects */
	entries=(char**)malloc(numentries*sizeof(char*));
	if (!entries) return 0;
	names=(char**)malloc(numentries*sizeof(char*));
	if (!names) {free(entries);return 0;}
	memset(entries,0,sizeof(char*)*numentries);
	memset(names,0,sizeof(char*)*numentries);

	/* Collect all the results into our arrays: */
	child=item->child;depth++;if (fmt) len+=depth;
	while (child)
	{
		names[i]=str=print_string_ptr(child->string);
		entries[i++]=ret=print_value(child,depth,fmt);
		if (str && ret) len+=strlen(ret)+strlen(str)+2+(fmt?2+depth:0); else fail=1;
		child=child->next;
	}

	/* Try to allocate the output string */
	if (!fail) out=(char*)malloc(len);
	if (!out) fail=1;

	/* Handle failure */
	if (fail)
	{
		for (i=0;i<numentries;i++) {if (names[i]) free(names[i]);if (entries[i]) free(entries[i]);}
		free(names);free(entries);
		return 0;
	}

	/* Compose the output: */
	*out='{';ptr=out+1;if (fmt)*ptr++='\n';*ptr=0;
	for (i=0;i<numentries;i++)
	{
		if (fmt) for (j=0;j<depth;j++) *ptr++='\t';
		strcpy(ptr,names[i]);ptr+=strlen(names[i]);
		*ptr++=':';if (fmt) *ptr++='\t';
		strcpy(ptr,entries[i]);ptr+=strlen(entries[i]);
		if (i!=numentries-1) *ptr++=',';
		if (fmt) *ptr++='\n';*ptr=0;
		free(names[i]);free(entries[i]);
	}

	free(names);free(entries);
	if (fmt) for (i=0;i<depth-1;i++) *ptr++='\t';
	*ptr++='}';*ptr++=0;
	return out;	
}

/* Get Array size/item / object item. */
int    json_array_size(json_t *array)							{json_t *c=array->child;int i=0;while(c)i++,c=c->next;return i;}
json_t *json_array_item(json_t *array,int item)				{json_t *c=array->child;  while (c && item>0) item--,c=c->next; return c;}
json_t *json_object_item(json_t *object,const char *string)	{json_t *c=object->child; while (c && cJSON_strcasecmp(c->string,string)) c=c->next; return c;}

/* Utility for array list handling. */
static void suffix_object(json_t *prev,json_t *item) {prev->next=item;item->prev=prev;}
/* Utility for handling references. */
static json_t *create_reference(json_t *item) {json_t *ref=cJSON_New_Item();if (!ref) return 0;memcpy(ref,item,sizeof(json_t));ref->string=0;ref->type|=JSON_IsReference;ref->next=ref->prev=0;return ref;}

/* Add item to array/object. */
void   json_array_add(json_t *array, json_t *item)						{json_t *c=array->child;if (!item) return; if (!c) {array->child=item;} else {while (c && c->next) c=c->next; suffix_object(c,item);}}
void   json_object_add(json_t *object,const char *string,json_t *item)	{if (!item) return; if (item->string) free(item->string);item->string=strdup(string);json_array_add(object,item);}
void	json_array_addref(json_t *array, json_t *item)						{json_array_add(array,create_reference(item));}
void	json_object_addref(json_t *object,const char *string,json_t *item)	{json_object_add(object,string,create_reference(item));}

json_t *json_array_detach(json_t *array,int which)			{json_t *c=array->child;while (c && which>0) c=c->next,which--;if (!c) return 0;
if (c->prev) c->prev->next=c->next;if (c->next) c->next->prev=c->prev;if (c==array->child) array->child=c->next;c->prev=c->next=0;return c;}
void   json_array_delete(json_t *array,int which)			{json_destroy(json_array_detach(array,which));}
json_t *json_object_detach(json_t *object,const char *string) {int i=0;json_t *c=object->child;while (c && cJSON_strcasecmp(c->string,string)) i++,c=c->next;if (c) return json_array_detach(object,i);return 0;}
void   json_object_delete(json_t *object,const char *string) {json_destroy(json_object_detach(object,string));}

/* Replace array/object items with new ones. */
void   json_array_replace(json_t *array,int which,json_t *newitem)		{json_t *c=array->child;while (c && which>0) c=c->next,which--;if (!c) return;
newitem->next=c->next;newitem->prev=c->prev;if (newitem->next) newitem->next->prev=newitem;
if (c==array->child) array->child=newitem; else newitem->prev->next=newitem;c->next=c->prev=0;json_destroy(c);}
void   json_object_replace(json_t *object,const char *string,json_t *newitem){int i=0;json_t *c=object->child;while(c && cJSON_strcasecmp(c->string,string))i++,c=c->next;if(c){newitem->string=strdup(string);json_array_replace(object,i,newitem);}}

/* Create basic types: */
json_t *json_null(void)					{json_t *item=cJSON_New_Item();if(item)item->type=JSON_NULL;return item;}
json_t *json_true(void)					{json_t *item=cJSON_New_Item();if(item)item->type=JSON_TRUE;return item;}
json_t *json_false(void)					{json_t *item=cJSON_New_Item();if(item)item->type=JSON_FALSE;return item;}
json_t *json_bool(int b)					{json_t *item=cJSON_New_Item();if(item)item->type=b?JSON_TRUE:JSON_FALSE;return item;}
json_t *json_number(double num)			{json_t *item=cJSON_New_Item();if(item){item->type=JSON_NUMBER;item->valuedouble=num;item->valueint=(int)num;}return item;}
json_t *json_string(const char *string)	{json_t *item=cJSON_New_Item();if(item){item->type=JSON_STRING;item->valuestring=strdup(string);}return item;}
json_t *json_array(void)					{json_t *item=cJSON_New_Item();if(item)item->type=JSON_ARRAY;return item;}
json_t *json_object(void)					{json_t *item=cJSON_New_Item();if(item)item->type=JSON_OBJECT;return item;}

/* Create Arrays: */
json_t *json_array_int(int *numbers,int count)				{int i;json_t *n=0,*p=0,*a=json_array();for(i=0;a && i<count;i++){n=json_number(numbers[i]);if(!i)a->child=n;else suffix_object(p,n);p=n;}return a;}
json_t *json_array_float(float *numbers,int count)			{int i;json_t *n=0,*p=0,*a=json_array();for(i=0;a && i<count;i++){n=json_number(numbers[i]);if(!i)a->child=n;else suffix_object(p,n);p=n;}return a;}
json_t *json_array_double(double *numbers,int count)		{int i;json_t *n=0,*p=0,*a=json_array();for(i=0;a && i<count;i++){n=json_number(numbers[i]);if(!i)a->child=n;else suffix_object(p,n);p=n;}return a;}
json_t *json_array_string(const char **strings,int count)	{int i;json_t *n=0,*p=0,*a=json_array();for(i=0;a && i<count;i++){n=json_string(strings[i]);if(!i)a->child=n;else suffix_object(p,n);p=n;}return a;}

/* Duplication */
json_t *json_dup(json_t *item,int recurse)
{
	json_t *newitem,*cptr,*nptr=0,*newchild;
	/* Bail on bad ptr */
	if (!item) return 0;
	/* Create new item */
	newitem=cJSON_New_Item();
	if (!newitem) return 0;
	/* Copy over all vars */
	newitem->type=item->type&(~JSON_IsReference),newitem->valueint=item->valueint,newitem->valuedouble=item->valuedouble;
	if (item->valuestring)	{newitem->valuestring=strdup(item->valuestring);	if (!newitem->valuestring)	{json_destroy(newitem);return 0;}}
	if (item->string)		{newitem->string=strdup(item->string);			if (!newitem->string)		{json_destroy(newitem);return 0;}}
	/* If non-recursive, then we're done! */
	if (!recurse) return newitem;
	/* Walk the ->next chain for the child. */
	cptr=item->child;
	while (cptr)
	{
		newchild=json_dup(cptr,1);		/* Duplicate (with recurse) each item in the ->next chain */
		if (!newchild) {json_destroy(newitem);return 0;}
		if (nptr)	{nptr->next=newchild,newchild->prev=nptr;nptr=newchild;}	/* If newitem->child already set, then crosswire ->prev and ->next and move on */
		else		{newitem->child=newchild;nptr=newchild;}					/* Set newitem->child and move to it */
		cptr=cptr->next;
	}
	return newitem;
}



static const unsigned char base64_enc_map[64] =
{
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd',
    'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
    'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', '+', '/'
};

static const unsigned char base64_dec_map[128] =
{
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127, 127, 127, 127, 127, 127, 127, 127,
    127, 127, 127,  62, 127, 127, 127,  63,  52,  53,
     54,  55,  56,  57,  58,  59,  60,  61, 127, 127,
    127,  64, 127, 127, 127,   0,   1,   2,   3,   4,
      5,   6,   7,   8,   9,  10,  11,  12,  13,  14,
     15,  16,  17,  18,  19,  20,  21,  22,  23,  24,
     25, 127, 127, 127, 127, 127, 127,  26,  27,  28,
     29,  30,  31,  32,  33,  34,  35,  36,  37,  38,
     39,  40,  41,  42,  43,  44,  45,  46,  47,  48,
     49,  50,  51, 127, 127, 127, 127, 127
};

/*
 * Encode a buffer into base64 format
 */
int base64_encode( unsigned char *dst, size_t *dlen,
                   const unsigned char *src, size_t slen )
{
    size_t i, n;
    int C1, C2, C3;
    unsigned char *p;

    if( slen == 0 )
        return( 0 );

    n = ( slen << 3 ) / 6;

    switch( ( slen << 3 ) - ( n * 6 ) )
    {
        case  2: n += 3; break;
        case  4: n += 2; break;
        default: break;
    }

    if( *dlen < n + 1 )
    {
        *dlen = n + 1;
        return( BASE64_ERR_BUFFER_TOO_SMALL );
    }

    n = ( slen / 3 ) * 3;

    for( i = 0, p = dst; i < n; i += 3 )
    {
        C1 = *src++;
        C2 = *src++;
        C3 = *src++;

        *p++ = base64_enc_map[(C1 >> 2) & 0x3F];
        *p++ = base64_enc_map[(((C1 &  3) << 4) + (C2 >> 4)) & 0x3F];
        *p++ = base64_enc_map[(((C2 & 15) << 2) + (C3 >> 6)) & 0x3F];
        *p++ = base64_enc_map[C3 & 0x3F];
    }

    if( i < slen )
    {
        C1 = *src++;
        C2 = ( ( i + 1 ) < slen ) ? *src++ : 0;

        *p++ = base64_enc_map[(C1 >> 2) & 0x3F];
        *p++ = base64_enc_map[(((C1 & 3) << 4) + (C2 >> 4)) & 0x3F];

        if( ( i + 1 ) < slen )
             *p++ = base64_enc_map[((C2 & 15) << 2) & 0x3F];
        else *p++ = '=';

        *p++ = '=';
    }

    *dlen = p - dst;
    *p = 0;

    return( 0 );
}

/*
 * Decode a base64-formatted buffer
 */
int base64_decode( unsigned char *dst, size_t *dlen,
                   const unsigned char *src, size_t slen )
{
    size_t i, n;
    uint32_t j, x;
    unsigned char *p;

    for( i = n = j = 0; i < slen; i++ )
    {
        if( ( slen - i ) >= 2 &&
            src[i] == '\r' && src[i + 1] == '\n' )
            continue;

        if( src[i] == '\n' )
            continue;

        if( src[i] == '=' && ++j > 2 )
            return( BASE64_ERR_INVALID_CHARACTER );

        if( src[i] > 127 || base64_dec_map[src[i]] == 127 )
            return( BASE64_ERR_INVALID_CHARACTER );

        if( base64_dec_map[src[i]] < 64 && j != 0 )
            return( BASE64_ERR_INVALID_CHARACTER );

        n++;
    }

    if( n == 0 )
        return( 0 );

    n = ( ( n * 6 ) + 7 ) >> 3;
    n -= j;

    if( dst == NULL || *dlen < n )
    {
        *dlen = n;
        return( BASE64_ERR_BUFFER_TOO_SMALL );
    }

   for( j = 3, n = x = 0, p = dst; i > 0; i--, src++ )
   {
        if( *src == '\r' || *src == '\n' )
            continue;

        j -= ( base64_dec_map[*src] == 64 );
        x  = ( x << 6 ) | ( base64_dec_map[*src] & 0x3F );

        if( ++n == 4 )
        {
            n = 0;
            if( j > 0 ) *p++ = (unsigned char)( x >> 16 );
            if( j > 1 ) *p++ = (unsigned char)( x >>  8 );
            if( j > 2 ) *p++ = (unsigned char)( x       );
        }
    }

    *dlen = p - dst;

    return( 0 );
}
//build a json_string with binary, caller must free json
json_t *json_base64str(byte *bin, size_t len){
	json_t* json;
	size_t dlen = len*8/6 + 10;
	char * dst = (char *)malloc(dlen);
	int rc = base64_encode(dst, &dlen, bin, len);
	assert(rc == 0); 
	json = json_string(dst);
	free(dst);

	return json;
}

//json must be a base64 json_string, caller must free bytes
byte* json_base64bin(json_t* json, size_t *len){
	char* str = json->valuestring;
	size_t slen = strlen(str);
	size_t dlen2 = (slen*6+10)/8;
	byte* bin = (byte*)malloc(dlen2);
	int rc = base64_decode(bin, len, str, slen);
	if( rc != 0){
		free(bin);
		*len = 0;
		return NULL;
	}
	return bin;
} 