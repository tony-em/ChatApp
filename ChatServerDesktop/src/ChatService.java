import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ChatService extends Thread {

    private Socket clientSocket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private volatile boolean enable;

    private static List<ChatService> clients = Collections.synchronizedList(new ArrayList<ChatService>());

    public ChatService(Socket socket) throws IOException {
        clientSocket = socket;
        dataInputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void run() {
        enable = true;

        try {
            clients.add(this);

            while (enable) {
                LocalTime localTime = LocalTime.now();
                String nowTime = localTime.getHour() + ":" + localTime.getMinute() + ":" + localTime.getSecond() + " > ";
                String msg = nowTime + dataInputStream.readUTF();
                sharing(msg);
            }

        } catch (IOException io) {
            System.out.println("Client " + this.clientSocket.getInetAddress() + " disconnected.");
        } finally {
            clients.remove(this);

            try {
                clientSocket.close();
                dataOutputStream.close();
                dataInputStream.close();
            } catch (IOException io) {
            }
        }
    }

    private static void sharing(String msg) {
        synchronized (clients) {
            Iterator<ChatService> iter = clients.iterator();
            while (iter.hasNext()) {
                ChatService client = iter.next();

                try {
                    synchronized (client.dataOutputStream) {
                        client.dataOutputStream.writeUTF(msg);
                    }
                    client.dataOutputStream.flush();
                } catch (IOException io) {
                    client.enable = false;
                }
            }
        }
    }
}
