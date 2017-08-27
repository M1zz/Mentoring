package yein.dc08;

import android.os.Environment;
import android.os.Message;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class SendFileThread extends Thread{
    String dstAddress;
    int dstPort;

    private boolean running;
    MainActivity.UdpClientHandler handler;

    InetAddress address;
    FileInputStream fis;

    int seq_num = 0;

    final BlockingQueue<DatagramPacket> queue;
    DatagramSocket socket;

    private static final int WINDOW_SIZE = 5;
    private static final int BUF_SIZE = 1024;

    MD5 md5 = new MD5();

    SendFileThread(String addr, int port, BlockingQueue<DatagramPacket> q, MainActivity.UdpClientHandler handler) {
        super();
        dstAddress = addr;
        dstPort = port;
        this.queue = q;
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
    private DatagramPacket takePacket(){
        try {
            return queue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }
    private void sendNotFilePacket(String str) throws IOException {
        byte[] buf;
        socket.setSoTimeout(1000);
        buf = sendFile(seq_num, str.getBytes());
        DatagramPacket packet = new DatagramPacket(buf, buf.length, address, dstPort);
        DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
        socket.send(packet);
        while(true) {
            try {
                socket.receive(receivedPacket);
                break;
            } catch (SocketTimeoutException e) {
                socket.send(packet);
            }
        }
        seq_num++;
    }
    @Override
    public void run() {
        sendState("connecting...");
        running = true;
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName(dstAddress);

            String fileName = "tree.jpg";
            String path = Environment.getExternalStorageDirectory() + "/" + fileName;
            File f = new File(path);
            fis = new FileInputStream(f);
            long file_size = f.length();

            DatagramPacket packet;

            sendNotFilePacket("start");

            String md5Value = md5.getMD5(path);
            String info = fileName + "," + Long.toString(file_size) + "," + md5Value;
            sendNotFilePacket(info);

            Log.d("md5_val", md5Value );

            byte[] send_buf = new byte[1024];
            byte[] tmp;
            int x;

            try {
                while (queue.size() <= WINDOW_SIZE) {
                    x = fis.read(send_buf, 0, 1020);
                    if (x == -1) break;
                    tmp = sendFile(seq_num, send_buf);
                    packet = new DatagramPacket(tmp, x + 4, address, dstPort);

                    if (!queue.offer(packet, 1000, TimeUnit.MILLISECONDS)) {
                        synchronized (queue) {
                            for (int i = 0; i < WINDOW_SIZE; i++) {
                                packet = takePacket();
                                socket.send(packet);
                                try {
                                    queue.put(packet);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    } else {
                        socket.send(packet);
                        seq_num++;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            sendNotFilePacket("end");

            handler.sendMessage( Message.obtain(handler, MainActivity.UdpClientHandler.UPDATE_MSG, "complete"));
        }catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            if(socket != null){
                socket.close();
                handler.sendEmptyMessage(MainActivity.UdpClientHandler.UPDATE_END);
            }
        }
    }
}
