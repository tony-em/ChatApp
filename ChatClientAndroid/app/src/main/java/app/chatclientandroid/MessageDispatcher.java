package app.chatclientandroid;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MessageDispatcher extends Fragment {

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    private String serverIp;
    private String serverPort;
    private String nickname;

    public static int CONNECT_TIMEOUT = 1000;
    public static int RECONNECT_TIMEOUT = 3000;

    private Handler msgDispatcher;
    private volatile Handler statusServerDispatcher;

    private volatile boolean isMyMsg = false;
    private boolean activeBuffer = false;

    private MessageReceiverListener receiverListener = null;
    private ServerStatusListener serverStatusListener = null;

    private ServerStatus thisStatus = ServerStatus.STATUS_FAIL;

    private Thread msgThread;
    private Thread serverStatusThread;


    public interface MessageReceiverListener {
        void messageReceive(String msg, MsgState state);
    }

    public void setOnMessageReceiverListener(MessageReceiverListener listener) {
        this.receiverListener = listener;
    }

    public void removeOnMessageReceiverListener() {
        this.receiverListener = null;
    }


    public interface ServerStatusListener {
        void status(ServerStatus status);
    }

    public void setOnServerStatusListener(ServerStatusListener listener) {
        this.serverStatusListener = listener;
    }

    public void removeOnServerStatusListener() {
        this.serverStatusListener = null;
    }


    public enum MsgState {
        STATE_MY, STATE_NOT_MY
    }

    public enum ServerStatus {
        STATUS_OK, STATUS_FAIL
    }


    public static MessageDispatcher newInstance(String serverIP, String serverPort, String nickname) {
        Bundle args = new Bundle();
        args.putString("serverIP", serverIP);
        args.putString("serverPort", serverPort);
        args.putString("nickname", nickname);

        MessageDispatcher fragment = new MessageDispatcher();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments().isEmpty())
            throw new NullPointerException("Use newInstance(...) method for create fragment");

        serverIp = getArguments().getString("serverIP");
        serverPort = getArguments().getString("serverPort");
        nickname = getArguments().getString("nickname");


        msgDispatcher = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (thisStatus == ServerStatus.STATUS_OK) {
                    String strMsg = (String) msg.obj;
                    MsgState state;

                    if (isMyMsg) {
                        state = MsgState.STATE_MY;
                        isMyMsg = false;
                    } else {
                        state = MsgState.STATE_NOT_MY;
                    }

                    receiveMsg(strMsg, state);
                    addMsgInBuff(strMsg, state);
                }
            }
        };

        statusServerDispatcher = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                setServerStatus(thisStatus);
            }
        };

        startMsgReceiverService();
        startServerStatusService();
    }

    public void restartServerInfo(String serverIp, String serverPort, String nickname) {
        stopMsgReceiverService();
        stopServerStatusService();

        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.nickname = nickname;

        System.gc();

        startMsgReceiverService();
        startServerStatusService();
    }


    private class ReceiverService implements Runnable {

        @Override
        public void run() {

            while (true) {

                while (!Thread.currentThread().isInterrupted()) {

                    try {
                        service();

                    } catch (Exception e1) {

                        try {
                            thisStatus = ServerStatus.STATUS_FAIL;

                            if (socket != null) socket.close();
                            if (dataInputStream != null) dataInputStream.close();
                            if (dataOutputStream != null) dataOutputStream.close();
                            Thread.sleep(RECONNECT_TIMEOUT);

                        } catch (Exception e2) {
                        }
                    }
                }
            }
        }

        private void service() throws Exception {
            if (socket == null || dataInputStream == null || dataOutputStream == null) {
                connect();
                thisStatus = ServerStatus.STATUS_OK;
                if (isMyMsg) isMyMsg = false;

            } else {

                try {
                    addMsg(dataInputStream.readUTF());
                } catch (IOException ioe) {
                    dataInputStream = null;
                    dataOutputStream = null;
                }
            }
        }
    }

    private void startMsgReceiverService() {
        msgThread = new Thread(new ReceiverService());
        msgThread.start();
    }

    private void stopMsgReceiverService() {
        msgThread.interrupt();
        msgThread = null;
    }

    private void addMsg(String strMsg) {
        Message msg = new Message();
        msg.obj = strMsg;
        msg.setTarget(msgDispatcher);
        msg.sendToTarget();
    }

    private void connect() throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(serverIp, Integer.parseInt(serverPort)), CONNECT_TIMEOUT);

        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    private void receiveMsg(String msg, MsgState state) {
        if (receiverListener != null) {
            receiverListener.messageReceive(msg, state);
        }
    }

    public boolean sendMsg(String msg) throws Exception {
        if (msg.trim().isEmpty()) return false;

        dataOutputStream.writeUTF(nickname + " " + msg.trim());
        isMyMsg = true;

        Thread transaction = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dataOutputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        transaction.start();
        transaction.interrupt();

        return true;
    }


    private void setServerStatus(ServerStatus status) {
        if (serverStatusListener != null) {
            serverStatusListener.status(status);
        }
    }

    private void startServerStatusService() {
        serverStatusThread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    while (!Thread.currentThread().isInterrupted()) {

                        statusServerDispatcher.sendEmptyMessage(0);

                        try {
                            Thread.sleep(250);
                        } catch (Exception ex) {
                        }
                    }
                }
            }
        });
        serverStatusThread.start();
    }

    private void stopServerStatusService() {
        serverStatusThread.interrupt();
        serverStatusThread = null;
    }


    private List<String[]> buffMsgs;
    private List<MessageDispatcher.MsgState> buffMsgsTypes;

    public void startMsgBuffer() {
        buffMsgs = new ArrayList<>();
        buffMsgsTypes = new ArrayList<>();
        activeBuffer = true;
    }

    public void flushBuffer(List<String[]> msgs, List<MessageDispatcher.MsgState> msgsStates) {
        if (buffMsgs.isEmpty() || buffMsgsTypes.isEmpty()) return;

        msgs.addAll(buffMsgs);
        msgsStates.addAll(buffMsgsTypes);
    }

    private void addMsgInBuff(String msg, MsgState state) {
        if (!activeBuffer) return;

        buffMsgs.add(parseMsg(msg));
        buffMsgsTypes.add(state);
    }

    public void stopMsgBuffer() {
        buffMsgs.clear();
        buffMsgsTypes.clear();
        buffMsgs = null;
        buffMsgsTypes = null;
        activeBuffer = false;
    }


    public static String[] parseMsg(String msg) {
        String[] parts = msg.split(" ", 3);
        parts[0] = parts[0].trim();
        parts[1] = parts[1].trim();
        parts[2] = parts[2].trim();

        return parts;
    }
}
