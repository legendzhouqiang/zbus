#include "MessageClient.h"

#include<iostream>
using namespace std;


int main(int argc, char* argv[]) { 
	ServerAddress addr("localhost:15555"); 

	cout << addr.address << endl;
	cout << Protocol::COMMAND << endl; 

	getchar();
}