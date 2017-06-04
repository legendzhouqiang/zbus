#ifndef __ZBUS_KIT_H__
#define __ZBUS_KIT_H__   
  
#include <cstring>   
#include <cstdlib> 

inline bool cmpIgnoreCase(char* s1, char* s2) {
	if (s1 == s2) return true;
	if (s1 == NULL) return false;
	if (s2 == NULL) return false;

	s1 = _strdup(s1);
	s2 = _strdup(s2);
	for (int i = 0; i < strlen(s1); i++) s1[i] = toupper(s1[i]);
	for (int i = 0; i < strlen(s2); i++) s2[i] = toupper(s2[i]);

	int res = strcmp(s1, s2);
	free(s1);
	free(s2);
	return res == 0;
}

inline char* strdupTrimed(char* str, int n) {
	char* p0 = str;
	char* p1 = str + n - 1;
	char* res;
	int len;
	while (*p0 == ' ' && p0<(str + n)) p0++;
	while (*p1 == ' ' && p1>str) p1--;
	len = p1 - p0 + 1;
	if (len<1) {
		return _strdup("");
	}
	res = (char*)malloc(len + 1);
	strncpy(res, p0, len);
	res[len] = '\0';
	return res;
} 


#endif