package app.chatclientandroid;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MessageDispatcher.MessageReceiverListener, MessageDispatcher.ServerStatusListener {

    private RecyclerView recyclerView;
    private MessageAdapter msgAdapter;
    private MessageDispatcher messageDispatcher;
    private EditText inputMsg;
    private Button sendMsgBtn;

    private boolean isDisabledInput = false;

    private static String SERVER_IP = "192.168.1.35";
    private static String SERVER_PORT = "8082";
    private static String NICKNAME = "Android";

    private static List<String[]> msgList;
    private static List<MessageDispatcher.MsgState> msgStatesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        FragmentManager fm = getSupportFragmentManager();
        messageDispatcher = (MessageDispatcher) fm.findFragmentByTag("MessageDispatcher");

        if (messageDispatcher == null) {
            messageDispatcher = MessageDispatcher.newInstance(SERVER_IP, SERVER_PORT, NICKNAME);
            fm.beginTransaction().add(messageDispatcher, "MessageDispatcher").commit();

            msgList = new ArrayList<>();
            msgStatesList = new ArrayList<>();
        } else {
            messageDispatcher.flushBuffer(msgList, msgStatesList);
            messageDispatcher.stopMsgBuffer();
        }

        messageDispatcher.setOnMessageReceiverListener(this);
        messageDispatcher.setOnServerStatusListener(this);


        inputMsg = (EditText) findViewById(R.id.input_msg);
        inputMsg.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        sendMsgBtn = (Button) findViewById(R.id.send_msg_btn);
        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputMsg.getText().toString().isEmpty()) return;

                try {
                    messageDispatcher.sendMsg(inputMsg.getText().toString());
                    inputMsg.getText().clear();
                    inputMsg.clearFocus();
                    scrollToEndMsg();
                } catch (Exception e) {
                    disableInput();
                }
            }
        });

        recyclerView = (RecyclerView) findViewById(R.id.msgs_pane);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        msgAdapter = new MessageAdapter(msgList, msgStatesList);
        recyclerView.setAdapter(msgAdapter);
    }

    private void enableInput() {
        inputMsg.setEnabled(true);
        sendMsgBtn.setEnabled(true);
        inputMsg.setHint("Write a message...");
        isDisabledInput = false;
    }

    private void disableInput() {
        inputMsg.getText().clear();
        inputMsg.setEnabled(false);
        sendMsgBtn.setEnabled(false);
        inputMsg.setHint("Server is not available. Please, try again...");
        isDisabledInput = true;
    }

    private void scrollToEndMsg() {
        recyclerView.scrollToPosition(msgAdapter.getItemCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        messageDispatcher.removeOnMessageReceiverListener();
        messageDispatcher.startMsgBuffer();
        messageDispatcher.removeOnServerStatusListener();
    }

    @Override
    public void messageReceive(String msg, MessageDispatcher.MsgState state) {
        if (state != MessageDispatcher.MsgState.STATE_ERROR) {
            msgList.add(MessageDispatcher.parseMsg(msg));
            msgStatesList.add(state);
            msgAdapter.setupData(msgList, msgStatesList);
            scrollToEndMsg();
        }
    }

    @Override
    public void status(MessageDispatcher.ServerStatus status) {
        if (status == MessageDispatcher.ServerStatus.STATUS_OK && isDisabledInput) {
            enableInput();
        } else if (status == MessageDispatcher.ServerStatus.STATUS_FAIL && !isDisabledInput) {
            disableInput();
        }
    }
}
