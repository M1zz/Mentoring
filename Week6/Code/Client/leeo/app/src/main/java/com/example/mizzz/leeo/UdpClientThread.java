package com.example.mizzz.leeo;
/*
	Date   : 2017.03.20
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StreamCorruptedException;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

class UdpClientThread extends Thread{
    String dstAddress;
    int dstPort;

    private boolean running;
    MainActivity.UdpClientHandler handler;

    DatagramSocket socket;
    InetAddress address;
    FileInputStream fis;

    int seq_num = 0;

    private static final int BUF_SIZE = 1024;

    UdpClientThread(String addr, int port, MainActivity.UdpClientHandler handler) {
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

    private int seqNum(String tmp){
        tmp = tmp.substring(0,4);
        int seqNum = Integer.parseInt(tmp);
        System.out.println( "seq_num : " + seqNum );
        return seqNum;
    }

    private boolean recvAck(int expected){
        try{
            byte[] buffer = new byte[1024];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivedPacket);
            String tmp = new String( receivedPacket.getData() );
            if(seqNum(tmp) == expected){
                seq_num++;
                return true;
            }else {
                return false;
            }
        }catch (Exception e){
            return false;
        }
    }

    private byte[] sendFile( int seq, byte[] data ){
        byte[] seq2byte = ByteBuffer.allocate(4).putInt(seq).array();
        try{
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(seq2byte);
            baos.write(data);
            return baos.toByteArray();
        }catch(Exception e){
            Log.d("what",e.toString());
            return null;
        }
    }

    @Override
    public void run() {
        sendState("connecting...");
        running = true;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(1000);
            address = InetAddress.getByName(dstAddress);

            String fileName = "background.jpg";
            File f = new File(Environment.getExternalStorageDirectory() +"/" + fileName );
            fis = new FileInputStream(f);
            long file_size = f.length();
            byte[] buf;
            DatagramPacket packet;

            do{
                buf = sendFile(seq_num, "start".getBytes());
                packet = new DatagramPacket(buf, buf.length, address, dstPort);
                socket.send(packet);
            }while(!this.recvAck(seq_num));

            do{
                String info = fileName + "," + Long.toString(file_size);
                buf = sendFile(seq_num, info.getBytes());
                packet = new DatagramPacket( buf, buf.length, address, dstPort);
                socket.send(packet);
            }while(!this.recvAck(seq_num));

            byte[] img_buf = new byte[1020];
            byte[] send;
            int x = fis.read(img_buf, 0, 1020);
            Log.d("int x", Integer.toString(x));
            while( x != -1 ) {
                do {
                    send = sendFile(seq_num, img_buf);
                    Log.d("seq_num", Integer.toString(seq_num));
                    packet = new DatagramPacket(send, x + 4, address, dstPort);
                    socket.send(packet);
                }while(!this.recvAck(seq_num));
                img_buf = new byte[1020];
                x = fis.read(img_buf, 0, 1020);
            }

            do{
                Log.d("end",Integer.toString(seq_num));
                buf = sendFile(seq_num, "end".getBytes());
                packet = new DatagramPacket( buf, buf.length, address, dstPort);
                socket.send(packet);
            }while(!this.recvAck(seq_num));

            handler.sendMessage( Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, "complete"));
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
