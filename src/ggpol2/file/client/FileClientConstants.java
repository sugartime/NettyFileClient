package ggpol2.file.client;

public class FileClientConstants {
	
	static final String TAG = "File Client";
	
	static final String 	HOST		= "127.0.0.1";
	
	static final boolean  	IS_SSL 		= false; //SSL 동작여부
	static final int 		PORT 		= 8023;
	static final int 		SSL_PORT 	= 8992;
	
	
	static final int 		INIT_BUF_SIZE 	= 512;  //파일전송전 보내는 파일내용 버퍼 크기
	static final int 		SND_BUF_SIZE 	= 4096;  //파일전송 버퍼 크기
	static final int 		POOL_BUF_SIZE 	= 1048576;  //풀링 버퍼사이즈 (1M)
	
	
	
	
}
