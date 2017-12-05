#include <netinet/in.h>
#include <stdio.h>
#include <netinet/ip_icmp.h>
#include <netinet/udp.h>
#include <netinet/tcp.h>
#include <netinet/ip.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#include <sys/types.h>

#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define PACKET_LENGTH  65536

void PrintPacket(unsigned char* , int);
void PrintTcp(unsigned char *, int size);
void PrintData (unsigned char *, int Size);

int main(int argc, char **argv)
{
	int readn;
	socklen_t addrlen;
	int sock_raw;
	struct sockaddr_in saddr;

	unsigned char *buffer = (unsigned char *)malloc(PACKET_LENGTH);

	sock_raw = socket(AF_INET , SOCK_RAW , IPPROTO_TCP);
	if(sock_raw < 0)
	{
		return 1;
	}
	while(1)
	{
		addrlen = sizeof(saddr);
		memset(buffer, 0x00, PACKET_LENGTH);
		readn = recvfrom(sock_raw , buffer , PACKET_LENGTH , 0 , (struct sockaddr *)&saddr , &addrlen);
		if(readn <0 )
		{
			return 1;
		}
		PrintPacket(buffer , readn);
	}
	close(sock_raw);
	return 0;
}

void PrintPacket(unsigned char* buffer, int size)
{
	struct iphdr *iph = (struct iphdr*)buffer;
	switch (iph->protocol)
	{
		case 1:
			printf("I recv icmp Packet\n");
			break;
		case 6:
			PrintTcp(buffer , size);
			break;
		case 17:
			printf("I recv UDP Packet\n");
			break;
		default:
			break;
	}
}

void PrintTcp(unsigned char* buf, int size)
{
	unsigned short iphdrlen;
	unsigned char *data;

	struct iphdr *iph = (struct iphdr *)buf;
	iphdrlen = iph->ihl*4;
	struct tcphdr *tcph=(struct tcphdr*)(buf + iphdrlen);

	data = (unsigned char *)(buf + (iph->ihl*4) + (tcph->doff*4));
	printf("%s", data);
}

