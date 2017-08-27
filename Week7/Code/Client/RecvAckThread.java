package yein.dc08;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

/**
 * Created by yein on 2017-06-06.
 */

class RecvAckThread extends Thread{
    private BlockingQueue<DatagramPacket> queue;
    private DatagramSocket socket;
    MainActivity.UdpClientHandler handler;

    RecvAckThread(BlockingQueue<DatagramPacket> q , MainActivity.UdpClientHandler handler) throws SocketException {
        this.queue = q;
        this.handler = handler;
    }

    @Override
    public void run() {
        int expected = 0;
        try {
            socket = new DatagramSocket(5017);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        while(true) {
            byte[] buffer = new byte[50];
            DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
            try {
                socket.receive(receivedPacket);
                String seq = new String(receivedPacket.getData());
                seq = seq.substring(0, 7);
                Integer ack = Integer.parseInt(seq);
                int loop = ack - expected;
                for( int i = 0 ; i <= loop ; i++ ){
                    queue.take();
                    expected = ack + 1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                socket.close();
                break;
            }
        }
    }
}
