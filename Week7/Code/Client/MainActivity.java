package yein.dc08;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST_STORAGE = 1;

    EditText editTextAddress, editTextPort;
    Button buttonSend;
    TextView textViewState, textViewRx;

    UdpClientHandler udpClientHandler;
    SendFileThread sendFileThread;
    RecvAckThread recvAckThread;

    private static final int WINDOW_SIZE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        buttonSend = (Button) findViewById(R.id.send);
        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.complete);

        udpClientHandler = new UdpClientHandler(this);
    }

    // SEND 버튼에 등록한 onClick 메서드, 어플리케이션의 권한관련
    public void send_btn(View v) throws SocketException {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Read/Write external storage", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSION_REQUEST_STORAGE);
        } else {
            send2server();
        }
    }

    private void send2server() throws SocketException {
        // 두개의 editText로부터 ip주소, 포트번호를 가져와서 String으로 변환
        String addr = editTextAddress.getText().toString();
        String port = editTextPort.getText().toString();

        // 두개의 String이 비어있지 않은지 확인, 비어있을경우 toast 메시지로 알림을 줌
        if(addr.getBytes().length  > 0 && port.getBytes().length > 0){
            int p = Integer.parseInt(port);

            BlockingQueue<DatagramPacket> queue = new ArrayBlockingQueue<>(WINDOW_SIZE);
            sendFileThread = new SendFileThread(addr, p, queue, udpClientHandler);
            recvAckThread = new RecvAckThread(queue, udpClientHandler);

            sendFileThread.start();
            recvAckThread.start();

            buttonSend.setEnabled(false); // CONNCET 버튼 비활성화
        }else{
            Toast.makeText(this, "주소와 포트번호를 채워주세요!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
    }

    private void clientEnd(){
        //sendFileThread = null;
        SendFileThread.interrupted();
        RecvAckThread.interrupted();
        textViewState.setText( "clientEnd()" );
        buttonSend.setEnabled(true);
    }

    public static class UdpClientHandler extends Handler {
        public static final int UPDATE_STATE = 0;
        public static final int UPDATE_MSG = 1;
        public static final int UPDATE_END = 2;
        private MainActivity parent;

        public UdpClientHandler(MainActivity parent) {
            super();
            this.parent = parent;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case UPDATE_STATE:
                    parent.updateState((String)msg.obj);
                    break;
                case UPDATE_MSG:
                    parent.updateRxMsg((String)msg.obj);
                    break;
                case UPDATE_END:
                    parent.clientEnd();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
