import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public void start(String serverAddress, int portNo) throws IOException {
        socket = new Socket(serverAddress, portNo);
        System.out.println("Connected to server");

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Thread for reading messages from the server
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String response;
                    while ((response = in.readLine()) != null) {
                        System.out.println("Server: " + response);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        // Thread for sending messages to the server
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
        socket.close();
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start("localhost", 5000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}