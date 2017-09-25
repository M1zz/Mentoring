#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int argc, char **argv)
{
	int i = 0;
	while(1)
	{
		printf("%d\n", i);
		i++;
		sleep(1);
	}
}

