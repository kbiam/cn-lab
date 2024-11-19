import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

class ChatServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public void start(int portNo) throws IOException {
        serverSocket = new ServerSocket(portNo);
        System.out.println("Server socket created");

        clientSocket = serverSocket.accept();
        System.out.println("Client connected");

        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Thread for reading messages from the client
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        System.out.println("Client: " + inputLine);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Thread for sending messages to the client
        try (Scanner scanner = new Scanner(System.in)) {
            String message;
            while (true) {
                System.out.print("Enter message: ");
                message = scanner.nextLine();
                out.println(message);
                if ("exit".equalsIgnoreCase(message)) {
                    break;
                }
            }
        }

        stop();
    }

    public void stop() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
        serverSocket.close();
    }
}

public class Server {
    public static void main(String[] args) throws IOException {
        ChatServer chatServer = new ChatServer();
        chatServer.start(5000);
    }
}