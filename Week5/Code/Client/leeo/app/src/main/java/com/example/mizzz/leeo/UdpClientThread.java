package com.example.mizzz.leeo;
/*
	Date   : 2017.03.15
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClientThread extends Thread{
    String dstAddress;
    int dstPort;

    private boolean running;
    MainActivity.UdpClientHandler handler;

    DatagramSocket socket;
    InetAddress address;
    FileInputStream fis;

    byte[] send = new byte[1024];

    public UdpClientThread(String addr, int port, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.handler = handler;
    }

    public void setRunning(boolean running){
        this.running = running;
    }

    private void sendState(String state){
        handler.sendMessage(
                Message.obtain(handler,
                        MainActivity.UdpClientHandler.UPDATE_STATE, state));
    }

    @Override
    public void run() {
        sendState("connecting...");
        running = true;

        try {
            // Datagram 소켓 생성
            socket = new DatagramSocket();
            // 문자열 IP주소를 가지고 InetAddress 객체를 생성
            address = InetAddress.getByName(dstAddress);
            String fileName = "Anne_of_Green_Gables.txt";
            File f = new File(Environment.getExternalStorageDirectory() +"/" + fileName );
            fis = new FileInputStream(f);

            // 문자열을 byte 배열로 변환
            String str = "start";
            byte[] buf = str.getBytes();

            // Datagram 패킷 생성
            DatagramPacket packet = new DatagramPacket( buf, buf.length, address, dstPort);
            // 패킷 전송
            socket.send(packet);

            str = fileName; // 전송할 파일 이름
            buf = str.getBytes();
            packet = new DatagramPacket( buf, buf.length, address, dstPort);
            socket.send(packet); // 패킷 전송

            // 파일을 읽어서 크기만큼 전송
            while( true ){
                int x = fis.read(send, 0, send.length);
                if( x == -1 )
                    break;
                packet = new DatagramPacket(send, x, address, dstPort);
                socket.send(packet);
            }
            // 파일 전송이 완료됨을 알림
            str = "end";
            byte[] t = str.getBytes();
            packet = new DatagramPacket(t,t.length , address, dstPort);
            socket.send(packet);

            handler.sendMessage(
                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, "complete"));

        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
            }
        }
    }
}
