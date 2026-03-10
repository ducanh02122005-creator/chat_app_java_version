import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {

    static final int PORT = 12345;

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(PORT);

        System.out.println("SERVER RUNNING...");

        while (true) {

            Socket client = server.accept();

            Thread thread =
                    new Thread(new ClientHandler(client));

            thread.start();
        }
    }
}