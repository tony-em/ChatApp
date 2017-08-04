import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientWindow extends JFrame implements Runnable {

    private Socket socket;
    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;
    private JTextArea messagesArea;
    private JTextField messageField;
    private JButton sendBtn;
    private String nickname;
    private static String serverIP;
    private static String serverPort;
    private volatile boolean enable;

    private static int RECONNECT_TIMEOUT = 3000;
    private static int TIMEOUT = 1000;

    public static void main(String[] args) {
        if (args.length != 3) {
            throw new RuntimeException("Set two params: <IP_SERVER> <PORT> <NICKNAME>");
        }

        initClient(args[0], args[1], args[2]);
    }

    private static void initClient(String ipServer, String port, String nickname) {
        Socket clientSocket;
        try {
            clientSocket = new Socket();
            clientSocket.connect(new InetSocketAddress(ipServer, Integer.parseInt(port)), TIMEOUT);
            System.out.println("Connecting successful!");
        } catch (Exception io) {
            clientSocket = null;
        }

        DataInputStream dataInputStream = null;
        DataOutputStream dataOutputStream = null;
        try {
            dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
            dataOutputStream = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));
        } catch (Exception io) {

            try {
                clientSocket.close();
                if (dataInputStream != null) dataInputStream.close();
            } catch (Exception e) {
            }
        }

        new ClientWindow(ipServer, port, clientSocket, dataInputStream, dataOutputStream, nickname);
    }

    public ClientWindow(String ipServer, String port, Socket socket,
                        DataInputStream dataInputStream, DataOutputStream dataOutputStream, String nickname) {
        super("ChatClientDesktop - Server[" + ipServer + ":" + port + "]");

        serverIP = ipServer;
        serverPort = port;

        this.socket = socket;
        this.dataInputStream = dataInputStream;
        this.dataOutputStream = dataOutputStream;
        this.nickname = nickname;

        initUI();

        new Thread(this).start();
    }

    private void initUI() {
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font("Century Gothic", Font.PLAIN, 18));
        messagesArea.setBackground(new Color(60, 63, 65));
        messagesArea.setForeground(new Color(205, 205, 205));
        JScrollPane scrollPane = new JScrollPane(messagesArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBackground(new Color(60, 63, 65));
        scrollPane.setBorder(null);
        messageField = new JTextField();
        messageField.setBackground(new Color(75, 79, 80));
        messageField.setForeground(new Color(160, 164, 165));
        messageField.setCaretColor(new Color(225, 225, 225));
        messageField.setFont(new Font("Century Gothic", Font.PLAIN, 18));
        messageField.setBorder(null);
        messageField.setText("Write a message...");
        DefaultCaret caret = (DefaultCaret) messagesArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        messageField.requestFocus();
        sendBtn = new JButton("Send");
        sendBtn.setFont(new Font("Century Gothic", Font.BOLD, 14));
        sendBtn.setMargin(new Insets(10, 20, 10, 20));
        sendBtn.setBackground(new Color(71, 182, 135));
        sendBtn.setForeground(new Color(245, 245, 245));
        sendBtn.setFocusPainted(false);
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(BorderLayout.CENTER, scrollPane);
        Panel panel = new Panel(new BorderLayout());
        panel.setBackground(new Color(75, 79, 80));
        panel.add(BorderLayout.CENTER, messageField);
        panel.add(BorderLayout.EAST, sendBtn);
        container.add(BorderLayout.SOUTH, panel);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(600, 600);
        setVisible(true);


        messageField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                messageField.setText("");
                if (messageField.getForeground() != new Color(225, 225, 225)) {
                    messageField.setForeground(new Color(205, 205, 205));
                }
            }
        });
        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMsg();
            }
        });
        sendBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (messageField.getText().equals("Write a message...")) return;
                sendMsg();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                enable = false;
                try {
                    dataInputStream.close();
                    dataOutputStream.close();
                    socket.close();
                } catch (IOException io) {
                    io.printStackTrace();
                }

                super.windowClosed(e);
            }
        });
    }

    private void sendMsg() {
        if (messageField.getText().equals("")) return;

        try {
            dataOutputStream.writeUTF(nickname + " > " + messageField.getText().trim());
            dataOutputStream.flush();
        } catch (IOException io) {
            enable = false;
        }

        messageField.setText("");
        messageField.requestFocus();
    }

    @Override
    public void run() {
        enable = true;
        boolean errFlag = false;
        boolean isBlocked = false;

        while (true) {

            if (socket != null && !errFlag) {
                try {

                    while (enable) {
                        String msg = dataInputStream.readUTF();
                        messagesArea.append(msg + "\n");
                    }
                } catch (Exception io) {
                    errFlag = true;
                }
            } else {

                if (!isBlocked) {
                    blocking();
                    System.out.println("Server is not available... Timeout " + RECONNECT_TIMEOUT + " ms.");
                    isBlocked = true;
                }

                try {
                    Thread.sleep(RECONNECT_TIMEOUT);
                    reconnect();
                    if (errFlag) errFlag = false;
                    isBlocked = false;
                } catch (Exception e) {
                }
            }
        }
    }

    private void reconnect() throws Exception {
        socket = new Socket(serverIP, Integer.parseInt(serverPort));
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));

        messageField.setEnabled(true);
        sendBtn.setEnabled(true);
        messageField.setText("");

        validate();
        System.out.println("Server is OK...");
    }

    private void blocking() {
        messageField.setEnabled(false);
        sendBtn.setEnabled(false);
        messageField.setText("Server is not available. Please, try again...");
        validate();
    }
}
