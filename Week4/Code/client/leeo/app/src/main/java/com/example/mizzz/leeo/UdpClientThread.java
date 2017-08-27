package com.example.mizzz.leeo;
/*
	Date   : 2017.03.10
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

import android.os.Message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UdpClientThread extends Thread{

    String dstAddress;
    int dstPort;
    String sendMessage;

    private boolean running;
    MainActivity.UdpClientHandler handler;

    DatagramSocket socket;
    InetAddress address;
    byte[] send = new byte[1024];
    byte[] recv = new byte[1024];

    public UdpClientThread(String addr, int port, String message, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        sendMessage = message;
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

            // 문자열을 byte 배열로 변환
            send = sendMessage.getBytes();
            // Datagram 패킷 생성
            DatagramPacket packet = new DatagramPacket(send, send.length, address, dstPort);
            // 패킷 전송
            socket.send(packet);

            sendState("connected");

            // 서버에서 전송한 Datagram packet을 receive
            packet = new DatagramPacket(recv, recv.length);
            socket.receive(packet);
            String line = new String( packet.getData(), 0, packet.getLength() );
            handler.sendMessage(
                    Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, line));

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

