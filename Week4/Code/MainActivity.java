/*
	Date   : 2017.03.10
	Author : Hyunholee
	e-mail : leeo75@cs-cnu.org
*/

package leeo.dc04;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    EditText editTextAddress, editTextPort, et_message;
    Button buttonConnect;
    TextView textViewState, textViewRx;

    UdpClientHandler udpClientHandler;
    UdpClientThread udpClientThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        et_message = (EditText)findViewById(R.id.message);
        buttonConnect = (Button) findViewById(R.id.connect);
        textViewState = (TextView)findViewById(R.id.state);
        textViewRx = (TextView)findViewById(R.id.received);

        udpClientHandler = new UdpClientHandler(this);
    }

    // connect 버튼에 등록한 onClick 메서드
    public void connect_btn(View v){
        // 세개의 editText로부터 ip주소, 포트번호, 전송할 메시지를 가져와서 String으로 변환
        String addr = editTextAddress.getText().toString();
        String port = editTextPort.getText().toString();
        String message = et_message.getText().toString();

        // 세개의 String이 비어있지 않은지 확인, 비어있을경우 toast 메시지로 알림을 줌
        if(addr.getBytes().length  > 0 && message.getBytes().length > 0 && port.getBytes().length > 0){
            int p = Integer.parseInt(port); // String 형의 포트번호를 int형으로 변환
            // UdpCliecntThread 생성하고 인자를 알맞게 구성해줌.
            udpClientThread = new UdpClientThread(addr, p ,message, udpClientHandler);
            udpClientThread.start(); // Thread.start()
            buttonConnect.setEnabled(false); // CONNCET 버튼 비활성화
        }else{
            Toast.makeText(this, "주소와 포트번호, 메시지 모두 채워주세요!", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateState(String state){
        textViewState.setText(state);
    }

    private void updateRxMsg(String rxmsg){
        textViewRx.append(rxmsg + "\n");
    }

    private void clientEnd(){
        udpClientThread = null;
        et_message.setText("");
        textViewState.setText( "clientEnd()" );
        buttonConnect.setEnabled(true);
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


