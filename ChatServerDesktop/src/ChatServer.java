import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    public ChatServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is run...");
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress());
                    new ChatService(socket).start();
                }
            } catch (IOException io) {
                io.printStackTrace();
            } finally {
                serverSocket.close();
            }

        } catch (IOException io) {
            io.printStackTrace();
            throw new RuntimeException("Server is not run! Set new server configuration.");
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            throw new RuntimeException("Set params server configuration for run: <PORT>");
        }

        new ChatServer(Integer.parseInt(args[0]));
    }
}
