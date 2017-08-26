/*
	Date   : 2017.03.10
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

import java.net.*;
import java.util.Arrays;
import java.util.Scanner;

class UDPServer{
	public static final int SERVERPORT = 6292;
	public static void main(String args[]) throws Exception{
		// DatagramSocket 생성
		DatagramSocket serverSock = new DatagramSocket(SERVERPORT); 
		System.out.println( "Socket is Created" );

		byte[] receiveData = new byte[1024]; // 받은 메시지를 저장할 byte배열
		byte[] sendData = new byte[1024]; // 보낼 메시지를 저장할 byte배열
		String sendMessage; // Scanner를 통해 입력받을 메시지 

		Scanner scan = new Scanner( System.in ); // 스캐너 객체 생성
		// 에러를 잡기위해 try/catch문으로 감쌈
		try{
			while( true ){
				// 클라이언트에서 전송한 DatagramPacket을 받기위한 receivePacket
				DatagramPacket receivePacket = new DatagramPacket( receiveData, receiveData.length );
				System.out.println( "Listening ..." );
				
				// 패킷을 받음
				serverSock.receive( receivePacket );
				// 받은 패킷에서 데이터를 sentence에 저장 
				String sentence = new String( receivePacket.getData() );
				System.out.println( "Received : " + sentence );	 // 받은 문자열을 출력 
				if( sentence.equals( "eixt" ) ) break;
				sentence = ""; // sentence 초기화
				Arrays.fill( receiveData, (byte)0); // receive Data 초기화

				System.out.print( "SEND : " ); 
				sendMessage = scan.nextLine(); // 클라이언트에게 보낼 문자열 입력 받음
				sendData = sendMessage.getBytes(); // 문자열을 byte형 배열로 변환
				//클라이언트에게 보낼 DatagramPacket 생성
				DatagramPacket sendPacket = new DatagramPacket( sendData, 
						sendData.length, 
						receivePacket.getAddress(), 
						receivePacket.getPort() );
				// 클라이언트에게 패킷 전송
				serverSock.send( sendPacket );
				System.out.println("");
			}
		}catch( Exception e ) {
			System.out.println( "occured error" );
		}finally{
			// 작업이 끝나면 서버소켓과 스캐너를 close
			if( serverSock != null )
				serverSock.close();
			System.out.println( "Close Server" );
			scan.close();
		}
	}
}
