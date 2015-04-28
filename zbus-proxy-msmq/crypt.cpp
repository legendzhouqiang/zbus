#include "crypt.h"

#include "platform.h"  
#include "json.h" 
#include "KDEncodeCli.h"  //金证交易加密
#include "encrypt.h"      //金证加密2
#include "CryptDLL2.h"    //网安解密 

static void
password_base64_decode(json_t* password_json, char* password_raw){
	size_t password_len;
	byte* password_bin = json_base64bin(password_json, &password_len);
	strncpy(password_raw, (char*) password_bin, password_len);
	free(password_bin);
}

static json_t* 
encrypt(json_t* params){
	json_t* json_res = json_object();
	json_t* algor_json = json_object_item(params, "algorithm");
	json_t* password_json = json_object_item(params, "password");
	json_t* key_json = json_object_item(params, "key");

	if(algor_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'algorithm' param"); 
		return json_res;
	}
	if(password_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'password' param"); 
		return json_res;
	}
	if(key_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'key' param"); 
		return json_res;
	}

	char * algor = algor_json->valuestring; 
	//char password[1024];
	//password_base64_decode(password_json, password); 
	char* password = password_json->valuestring;
	char* key = key_json->valuestring;
	char res[1024] = {0};

	if(strcmp(algor, "KDE") == 0){
		KDEncode(KDCOMPLEX_ENCODE,(unsigned char*)password, strlen(password),
			(unsigned char*)res, 64, key, strlen(key));
		json_object_addstr(json_res, "result", res); 
		printf("res==>%s\n",res);
	} else if(strcmp(algor,"AES") == 0){
		long long llkey = _atoi64(key);
		AES_Encrypt1(res, sizeof(res), llkey, password);
		json_object_addstr(json_res, "result", res);
	} else {
		json_object_addstr(json_res, "error_code", "404");
		json_object_addstr(json_res, "error_msg", "encrypt algorithm not support"); 
	}
	return json_res;
}

static json_t* 
decrypt(json_t* params){
	json_t* json_res = json_object();
	json_t* algor_json = json_object_item(params, "algorithm");
	json_t* public_json = json_object_item(params, "public");
	json_t* private_json = json_object_item(params, "private");
	json_t* password_json = json_object_item(params, "password");

	if(algor_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'algorithm' param"); 
		return json_res;
	}
	if(public_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'public' param"); 
		return json_res;
	}
	if(private_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'private' param"); 
		return json_res;
	}
	if(password_json == NULL){
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'password' param"); 
		return json_res;
	}


	char* algor_str = algor_json->valuestring;
	char* public_str = public_json->valuestring;
	char* private_str = private_json->valuestring; 
	char* password_str = password_json->valuestring;
	//char password_str[1024];
	//password_base64_decode(password_json, password_str); 

	char res[1024] = {0};

	if(strcmp(algor_str, "WANGAN") == 0){
		int len = DecryptStr(public_str, private_str, password_str, res, sizeof(res));
		if(len == -1){ 
			json_object_addstr(json_res, "error_code", "500");
			json_object_addstr(json_res, "error_msg", "decrpyt error");
		} else {
			res[len] = '\0';
			json_object_addstr(json_res, "result", res);
		}
	} else {
		json_object_addstr(json_res, "error_code", "404");
		json_object_addstr(json_res, "error_msg", "decrypt algorithm not support"); 
	} 

	return json_res;
}

msg_t* crypt_handler(msg_t* req){
	//{ method: encrypt|decrypt, params:{algorithm:KDE|WANGAN, } }
	msg_t* res = msg_new();
	msg_set_mq_reply(res, msg_get_mq_reply(req));
	msg_set_msgid(res, msg_get_msgid_raw(req));

	char* bodystr = msg_copy_body(req);
	json_t* json = json_parse(bodystr); 
	msg_destroy(&req);
	free(bodystr);
	
	json_t* json_res = NULL;
	json_t* method_json = json_object_item(json, "method");
	if(method_json == NULL){
		json_res = json_object();
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'method'"); 
		goto destroy;
	}
	
	json_t* params_json = json_object_item(json, "params");
	if(params_json == NULL){
		json_res = json_object();
		json_object_addstr(json_res, "error_code", "400");
		json_object_addstr(json_res, "error_msg", "missing 'params'"); 
		goto destroy;
	}

	char* method = method_json->valuestring; 
	if(strcmp(method, "encrypt") == 0){
		json_res = encrypt(params_json);
	} else if(strcmp(method, "decrypt") == 0){
		json_res = decrypt(params_json);
	} else {
		json_res = json_object();
		json_object_addstr(json_res, "error_code", "404");
		json_object_addstr(json_res, "error_msg", "command not support"); 
	}

destroy:
	json_destroy(json);
	msg_set_status(res, "200");
	
	char* json_res_str = json_dump(json_res);
	msg_set_json_body(res, json_res_str);
	free(json_res_str);

	json_destroy(json_res);

	return res;
}