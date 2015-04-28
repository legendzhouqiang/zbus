#include <stdio.h>
#include <stdlib.h> 
#include <assert.h>
#include <Windows.h> 

#import "mqoa.dll"
using namespace MSMQ;

const char* g_sn_delimit = "~_*&#^#&*_~";


IMSMQQueuePtr msmq_open(IMSMQQueueInfoPtr qinfo, enum MQACCESS access){
	IMSMQQueuePtr queue;
	try{
		queue = qinfo->Open(access, MQ_DENY_NONE);
	}catch(_com_error& e){
		wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
		try{
			_variant_t IsTransactional(false);
			_variant_t IsWorldReadable(true);
			qinfo->Create(&IsTransactional, &IsWorldReadable);
			queue = msmq_open(qinfo , access);
		}catch (_com_error& e){ 
			wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
			assert(0);
		} 
	}
	return queue;
}


void msmq_close(IMSMQQueuePtr queue){
	queue->Close(); 
}


int msmq_send(IMSMQQueuePtr queue, char* msg, int timeout){
	IMSMQMessagePtr pmsg("MSMQ.MSMQMessage");  
	pmsg->Body = msg;
	pmsg->MaxTimeToReachQueue = timeout;
	pmsg->MaxTimeToReceive = timeout;
	try{
		pmsg->Send(queue);
	}catch(_com_error& e){ 
		wprintf(L"Error Code = 0x%X\nError Description = %s\n", e.Error(), (wchar_t *)e.Description());
		return -1;
	} 
	return 0;
}


IMSMQMessagePtr msmq_recv(IMSMQQueuePtr queue, int timeout){
	IMSMQMessagePtr	pmsg("MSMQ.MSMQMessage");
	_variant_t	vtimeout((long)timeout); 
	_variant_t  want_body((bool)true); 
	pmsg = queue->Receive(&vtMissing, &vtMissing, &want_body, &vtimeout);
	return pmsg;
}


char*
option(int argc, char* argv[], char* opt, char* default_value){
	int i,len;
	char* value = default_value;
	for(i=1; i<argc; i++){
		len = strlen(opt);
		if(len> strlen(argv[i])) len = strlen(argv[i]);
		if(strncmp(argv[i],opt,len)==0){
			value = &argv[i][len];
		}
	}
	return value;
}
 

int main(int argc, char* argv[]){
	::CoInitializeEx(NULL,COINIT_MULTITHREADED); 

	char* server_ip = option(argc, argv, "-msmq_s", "127.0.0.1");
	char* client_ip = option(argc, argv, "-msmq_c", "127.0.0.1");
	int timeout =  atoi(option(argc, argv, "-msmq_t", "10000")); 

	char client_ip_[100];
	sprintf(client_ip_, "%s", client_ip); 
	for(int i=0;i<strlen(client_ip_);i++) if(client_ip_[i] == '.') client_ip_[i] = '_';


	char sendmq[256];
	sprintf(sendmq, "DIRECT=TCP:%s\\PRIVATE$\\%s_send", client_ip, client_ip_);
	printf("%s\n", sendmq);
	IMSMQQueueInfoPtr qinfo_send = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
	qinfo_send->FormatName = _bstr_t(sendmq);
	qinfo_send->Label = _bstr_t(sendmq);
	IMSMQQueuePtr producer = msmq_open(qinfo_send, MQ_SEND_ACCESS); 


	char recvmq[256];
	sprintf(recvmq, ".\\PRIVATE$\\%s_recv", client_ip_);
	IMSMQQueueInfoPtr qinfo_recv = IMSMQQueueInfoPtr("MSMQ.MSMQQueueInfo");
	qinfo_recv->PathName = _bstr_t(recvmq);
	qinfo_recv->Label = _bstr_t(recvmq);
	IMSMQQueuePtr consumer = msmq_open(qinfo_recv, MQ_RECEIVE_ACCESS); 
	
	IMSMQMessagePtr	pmsg("MSMQ.MSMQMessage"); 
	while(1){  
		pmsg = msmq_recv(consumer, timeout); 
		if(pmsg == NULL) continue;   
		
		_bstr_t body = pmsg->Body;  

		char* msg = (char*)body;  
		printf("MSMQ Recv: %s\n", msg);
	
		msmq_send(producer, msg, timeout);
	}

	::CoUninitialize();
	return 0;
}