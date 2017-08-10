package app.chatclientandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter msgAdapter;
    private MessageDispatcher messageDispatcher;
    private AlertDialog alertDialog;
    private EditText inputMsg;
    private ImageButton sendMsgBtn;
    private ProgressDialog progressDialog;

    private boolean isDisabledInput = false;

    private String serverIp;
    private String serverPort;
    private String nickname;

    private static List<String[]> msgList;
    private static List<MessageDispatcher.MsgState> msgStatesList;

    private MessageDispatcher.ServerStatus serverStatus = MessageDispatcher.ServerStatus.STATUS_FAIL;
    private boolean isValidServer = false;
    private boolean clickListenerFlag = false;

    private boolean msgNotifServiceFlag = false;

    private long backPressTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            isValidServer = savedInstanceState.getBoolean("validServerFlag");
        }

        FragmentManager fm = getSupportFragmentManager();
        messageDispatcher = (MessageDispatcher) fm.findFragmentByTag("MessageDispatcher");

        if (messageDispatcher == null) {
            msgList = new ArrayList<>();
            msgStatesList = new ArrayList<>();
        } else {
            messageDispatcher.flushBuffer(msgList, msgStatesList);
            messageDispatcher.stopMsgBuffer();

            if (isValidServer) {
                messageDispatcher.setOnMessageReceiverListener(messageReceiverListener);
                messageDispatcher.setOnServerStatusListener(serverStatusListener);
            } else {
                messageDispatcher.setOnServerStatusListener(serverStatusListener);
            }
        }


        inputMsg = (EditText) findViewById(R.id.input_msg);
        inputMsg.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        });

        sendMsgBtn = (ImageButton) findViewById(R.id.send_msg_btn);
        sendMsgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (inputMsg.getText().toString().isEmpty()) return;

                try {
                    if (!messageDispatcher.sendMsg(inputMsg.getText().toString())) return;
                    inputMsg.getText().clear();
                    inputMsg.clearFocus();
                    scrollToEndMsg();
                } catch (Exception e) {
                    disableInput();
                }
            }
        });

        if (!isValidServer) {
            disableInput();
        }

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting...");
        progressDialog.setCancelable(false);

        recyclerView = (RecyclerView) findViewById(R.id.msgs_pane);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        msgAdapter = new MessageAdapter(msgList, msgStatesList);
        recyclerView.setAdapter(msgAdapter);

        createServerInformationDialog();
    }

    private void createServerInformationDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle("Enter server information").setCancelable(false);
        getLayoutInflater().inflate(R.layout.server_inf, null);

        dialogBuilder.setView(getLayoutInflater().inflate(R.layout.server_inf, null))
                .setPositiveButton("Connect", null)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        showToast("Cancel");
                    }
                })
                .setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        if (i == KeyEvent.KEYCODE_BACK && keyEvent.getAction() == KeyEvent.ACTION_UP) {
                            delayExit();
                        }

                        return false;
                    }
                });

        alertDialog = dialogBuilder.create();

        if (!isValidServer) {
            showServerInformationDialog();
        }
    }

    private void showServerInformationDialog() {
        alertDialog.show();

        if (!clickListenerFlag) {
            clickListenerFlag = true;
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new ValidServerListener());
        }
    }

    private void hideServerInformationDialog() {
        alertDialog.dismiss();
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
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
        inputMsg.setHint("Server is not available...");
        isDisabledInput = true;
    }

    private void scrollToEndMsg() {
        recyclerView.scrollToPosition(msgAdapter.getItemCount() - 1);
    }


    private void startMsgNotificationService(String[] msg) {
        Intent intent = new Intent(this, MessageNotificationService.class);

        if (msg == null) {
            startService(intent);
        } else {
            startService(intent.putExtra("msgNotification", msg));
        }
    }

    private void stopMsgNotificationService() {
        Intent intent = new Intent(this, MessageNotificationService.class);
        stopService(intent);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("validServerFlag", isValidServer);
    }

    @Override
    public void onBackPressed() {
        delayExit();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (msgNotifServiceFlag) {
            stopMsgNotificationService();
            msgNotifServiceFlag = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (!msgNotifServiceFlag) {
            startMsgNotificationService(null);
            msgNotifServiceFlag = true;
        }
    }

    @Override
    protected void onDestroy() {
        if (messageDispatcher != null) {
            messageDispatcher.removeOnMessageReceiverListener();
            messageDispatcher.startMsgBuffer();
            messageDispatcher.removeOnServerStatusListener();
        }

        if (msgNotifServiceFlag) {
            stopMsgNotificationService();
            msgNotifServiceFlag = false;
        }

        super.onDestroy();
    }

    private void delayExit() {
        if (backPressTime + 2000 > System.currentTimeMillis()) {
            if (alertDialog.isShowing()) hideServerInformationDialog();
            exit();
        } else {
            showToast("Click again to exit");
            backPressTime = System.currentTimeMillis();
        }
    }

    private void exit() {
        finish();
        System.exit(0);
    }


    private MessageDispatcher.MessageReceiverListener messageReceiverListener = new MessageDispatcher.MessageReceiverListener() {
        @Override
        public void messageReceive(String msg, MessageDispatcher.MsgState state) {
            String[] resultMsg = MessageDispatcher.parseMsg(msg);

            msgList.add(MessageDispatcher.parseMsg(msg));
            msgStatesList.add(state);
            msgAdapter.setupData(msgList, msgStatesList);
            scrollToEndMsg();

            if (msgNotifServiceFlag) {
                startMsgNotificationService(resultMsg);
            }
        }
    };

    private MessageDispatcher.ServerStatusListener serverStatusListener = new MessageDispatcher.ServerStatusListener() {
        @Override
        public void status(MessageDispatcher.ServerStatus status) {
            serverStatus = status;

            if (status == MessageDispatcher.ServerStatus.STATUS_OK && isDisabledInput) {
                enableInput();
            } else if (status == MessageDispatcher.ServerStatus.STATUS_FAIL && !isDisabledInput) {
                disableInput();
            }
        }
    };


    private class ValidServerListener implements View.OnClickListener {

        private EditText ipEt;
        private EditText portEt;
        private EditText nicknameEt;

        public ValidServerListener() {
            ipEt = (EditText) alertDialog.findViewById(R.id.ip);
            portEt = (EditText) alertDialog.findViewById(R.id.port);
            nicknameEt = (EditText) alertDialog.findViewById(R.id.nickname);
        }

        @Override
        public void onClick(View view) {
            ipEt.setText("192.168.1.35");
            portEt.setText("8082");
            nicknameEt.setText("TestAndroid");

            serverIp = ipEt.getText().toString();
            serverPort = portEt.getText().toString();
            nickname = nicknameEt.getText().toString();

            if (serverIp.isEmpty() || serverPort.isEmpty() || nickname.isEmpty()) {
                showToast("Enter all information");
            } else {

                if (messageDispatcher == null) {
                    messageDispatcher = MessageDispatcher.newInstance(serverIp, serverPort, nickname);
                    getSupportFragmentManager().beginTransaction().add(messageDispatcher, "MessageDispatcher").commit();
                    messageDispatcher.setOnServerStatusListener(serverStatusListener);
                } else {
                    messageDispatcher.restartServerInfo(serverIp, serverPort, nickname);
                }

                new ValidServerProcess().execute();
            }
        }
    }

    private class ValidServerProcess extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            hideServerInformationDialog();
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            progressDialog.dismiss();
            if (serverStatus == MessageDispatcher.ServerStatus.STATUS_OK) {
                messageDispatcher.setOnMessageReceiverListener(messageReceiverListener);
                showToast("Connecting successful");
                isValidServer = true;
            } else {
                showServerInformationDialog();
                showToast("Server is not available");
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            int timeout = MessageDispatcher.RECONNECT_TIMEOUT;
            long timer = System.currentTimeMillis() + timeout;

            while (serverStatus == MessageDispatcher.ServerStatus.STATUS_FAIL && System.currentTimeMillis() < timer) {
                continue;
            }

            return null;
        }
    }
}
