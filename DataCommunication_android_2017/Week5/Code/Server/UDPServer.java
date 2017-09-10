import java.io.*;
import java.net.*;

class UDPServer{
	public static final int SERVERPORT =6292;
	public static void main(String args[] ) throws Exception{
		try{
			// DatagramSocket 생성
			DatagramSocket serverSock = new DatagramSocket(SERVERPORT);
			System.out.println( "Socket is Created" );

			File f = null; // 빈 파일 선언
			DataOutputStream dos = null; // 데이터를 바이트 스트림으로 출력

			while(true){
				byte[] buf = new byte[1024];
				// 클라이언트에서 전송한 DatagramPacket을 받기위한 receivePacket
				DatagramPacket receivePacket = new DatagramPacket( buf, buf.length );
				serverSock.receive( receivePacket ); // 패킷을 받음
				// 받은 패킷에서 데이터를 str 변수에 저장
				String str = new String( receivePacket.getData() ); 
				
				// 패킷의 내용이 start일경우
				if( str.trim().equals( "start" ) ){
					System.out.println( "Start to get file" );
					// 서버에서 전송한 파일이름을 받아서 파일 객체 생성
					receivePacket = new DatagramPacket( buf, buf.length );
					serverSock.receive( receivePacket );
					String str2 = new String( receivePacket.getData() );
					f = new File( "./" + str2.trim() );
				
					System.out.println(str2);
					// 파일을 쓰기위해 객체 생성
					dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(f) ) );
				}else if( str.trim().equals( "end" ) ){ // 받은 패킷의 내용이 end일경우 
					System.out.println( "Finish" );
					// 소켓을 닫고 break;
					dos.close(); 
					serverSock.close();
					break;
				}else{
					// dos 객체에 연결된 파일에 받은 패킷의 내용인 str의 길이만큼 기록
					System.out.println("writing..");
					dos.write( str.getBytes(), 0, str.getBytes().length );
				}
			}
		}catch( Exception e ){
			System.out.println( "Error" );
		}
	}
}
