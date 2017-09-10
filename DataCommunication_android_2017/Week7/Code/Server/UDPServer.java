import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

public class UDPServer {

	static final int SERVERPORT = 2080;
	public static void main(String args[] ) throws Exception{
		Server s = new Server();
		if( s.recvFileInfo() ){
			System.out.println( "Start Receive Image File" );
			if( s.startRecv() ){
				System.out.println( "<< Finished >>" );
			}else{
				System.out.println( "MD5 Value is different. Try Again");
			}
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
	MD5 md5 = new MD5();
	
	String fileName;
	float fileSize;
	float curSize = 0;
	
	String md5value;

	Server() {
		try{
			serverSock = new DatagramSocket(UDPServer.SERVERPORT);
		}catch(Exception e){
			System.out.println( e );
		}
	}

	boolean recvFileInfo(){
		try{
			System.out.println("Socket is Created Wating Start Packet");
			byte[] recv = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(recv, recv.length);
			while (true) {	
				serverSock.receive(receivePacket);
				recv = receivePacket.getData();
				String message = this.getMessage(receivePacket);

				if (message.equals("start") && checkSeqNum(receivePacket.getData())) {
					System.out.println("Start to get file infomation");
					this.sendAckStartAndEnd(receivePacket);
				} else {
					System.out.println("First Packet is not Start Packet try again");
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
					
					this.md5value = tmp[2].trim();
					System.out.println(this.md5value);
					this.sendAckStartAndEnd(receivePacket);
					
					System.out.println( "fileName : " + this.fileName ); 
				}else{
					return false;
				}
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
	
	boolean checkMD5() throws NoSuchAlgorithmException, FileNotFoundException{
		String recvFile = md5.getMD5("./" + this.fileName);
		if( recvFile.equals( this.md5value )){
			System.out.println( "Original File MD5 : " + this.md5value );
			System.out.println( "Received File MD5 : " + recvFile );
			System.out.println( "Equal" );
			return true;
		}else{
			System.out.println( "Original File MD5 : " + this.md5value );
			System.out.println( "Received File MD5 : " + recvFile );
			System.out.println( "Different" );
			return false;
		}
		
	}
	
	boolean startRecv(){
		try{
			while(true){
				byte[] recv = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket( recv, recv.length );
				serverSock.receive( receivePacket );
				recv = receivePacket.getData();
				while( this.checkSeqNum( recv ) == false ){
					System.out.println( "received wrong seq. recv again. expected : " + this.expectedSeqNum );
					serverSock.receive( receivePacket );
					recv = receivePacket.getData();
				}
				String msg = this.getMessage( receivePacket );
				if( msg.equals( "end" ) ){
					this.sendAckStartAndEnd( receivePacket );
					this.serverSock.close();
					this.dos.close();
					System.out.println( "<< Received is complete >>" );
					System.out.println( "<< Start check MD5 value >>");
					if( checkMD5() ){
						return true;
					}else{
						return false;
					}
				}

				this.sendAckPacket( receivePacket );
				byte write[] = this.getData( receivePacket );
				this.dos.write( write );
				curSize = curSize + write.length;
				printPercentage(curSize);
			}
		}catch(Exception e ){
			System.out.println( "Error while received" );
			return false;
		}
	}

	private void printPercentage( float curSize ){
		String info = String.format( "%.0f / %.0f ( current size / total size ), %.3f%%", 
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
			String ack = String.format( "%07d", expectedSeqNum -1 );
			recv = ack.getBytes();			
			DatagramPacket sendPacket = new DatagramPacket( recv, 
					recv.length, 
					recvPacket.getAddress(),
					5017 );
			serverSock.send( sendPacket );
		}catch( Exception e ){
			System.out.println( "Error while sending Ack Packet" );
		}
	}
	private void sendAckStartAndEnd(DatagramPacket recvPacket){
		try{
			byte[] recv = new byte[10];
			String ack = String.format( "%07d", expectedSeqNum -1 );
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
