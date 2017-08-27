import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.nio.ByteBuffer;

class UDPServer{
	static final int SERVERPORT = 2080;
	public static void main(String args[] ) throws Exception{
		Server s = new Server();
		if( s.recvFileInfo() ){
			System.out.println( "Start Receive Image File" );
			long startTime = System.currentTimeMillis();
			s.startRecv();
			long endTime = System.currentTimeMillis();
			long lTime = endTime - startTime;
			System.out.println("TIME : " + lTime/1000.0);
		}else{
			System.out.println( "Error occurred while receiving file info, try again" );
		}
	}
}

class Server{
	DatagramSocket serverSock;
	File f = null;
	DataOutputStream dos = null;
	int expectedSeqNum = 0;
	
	String fileName;
	float fileSize;
	float curSize = 0;

	Server() {
		try{
			serverSock = new DatagramSocket(UDPServer.SERVERPORT);
		}catch(Exception e){
			System.out.println( "socket create error" );
		}
	}

	boolean recvFileInfo(){
		try{
			System.out.println( "Socket is Created Wating Start Packet" );

			byte[] recv = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket( recv, recv.length );
			serverSock.receive( receivePacket );
			recv = receivePacket.getData();
			String message = this.getMessage( receivePacket );

			if( message.equals( "start" ) && checkSeqNum( receivePacket.getData() )){
				System.out.println( "Start to get file infomation" );
				this.sendAckPacket(receivePacket);
			}else{
				System.out.println( "First Packet is not Start Packet try again" );
				return false;
			}

			serverSock.receive( receivePacket );
			if( checkSeqNum( receivePacket.getData() ) ){
				String str = this.getMessage( receivePacket );
				String[] tmp = str.split( ",");
				this.fileName = tmp[0].trim();
				this.f = new File( "./" + this.fileName );
				this.fileSize = Float.valueOf( tmp[1].trim() );
				this.dos = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(f) ) );
				this.sendAckPacket(receivePacket);
				System.out.println( "fileName : " + this.fileName ); 
			}else{
				return false;
			}
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	void startRecv(){
		try{
			while(true){
				byte[] recv = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket( recv, recv.length );
				serverSock.receive( receivePacket );
				recv = receivePacket.getData();
				while( this.checkSeqNum( recv ) == false ){
					System.out.println( "received wrong seq. recv again." );
					serverSock.receive( receivePacket );
					recv = receivePacket.getData();
					this.sendAckPacket( receivePacket );
				}
				String msg = this.getMessage( receivePacket );
				if( msg.equals( "end" ) ){
					this.sendAckPacket( receivePacket );
					this.serverSock.close();
					this.dos.close();
					System.out.println("finished");
					break;
				}
				this.sendAckPacket( receivePacket );
				byte write[] = this.getData( receivePacket );
				this.dos.write( write );
				curSize = curSize + write.length;
				printPercentage(curSize);
			}
		}catch(Exception e ){
			System.out.println( e );
		}
	}

	private void printPercentage( float curSize ){
		String info = String.format( "%.0f / %.0f ( current size / total size ), %.2f%%", 
									curSize, fileSize, curSize/fileSize*100 );
		System.out.println(info);
	}

	private String getMessage( DatagramPacket packet ){
		String recv = new String( packet.getData() );
		String message = recv.substring(4, recv.length());
		return message.trim();
	}

	private void sendAckPacket(DatagramPacket recvPacket){
		try{
			byte[] recv = new byte[10];
			String ack = String.format( "%04d", expectedSeqNum -1 );
			recv = ack.getBytes();			
			DatagramPacket sendPacket = new DatagramPacket( recv, 
					recv.length, 
					recvPacket.getAddress(), 
					recvPacket.getPort() );
			serverSock.send( sendPacket );
		}catch( Exception e ){
			System.out.println( "Error while sending Ack Packet" );
		}
	}

	private boolean checkSeqNum( byte[] recv ){
		int tmp = this.getSeqNum( recv );
//		System.out.println( tmp  + " " + this.expectedSeqNum );
		if( tmp == this.expectedSeqNum ){
			this.expectedSeqNum++;
			return true;
		}else{
			return false;
		}
	}

	private int getSeqNum( byte[] recv ){
		byte[] seq = Arrays.copyOfRange(recv, 0, 4);
		int seqNum = ByteBuffer.wrap(seq).getInt();
		return seqNum;
	}

	private byte[] getData(DatagramPacket recvPacket){
		byte[] received = recvPacket.getData();
		return Arrays.copyOfRange(received, 4, recvPacket.getLength() );
	}
}
