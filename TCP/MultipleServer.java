import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class ChatServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final AtomicInteger clientCount = new AtomicInteger(0);
    private static final int MAX_CLIENTS = 4;
    private volatile boolean isRunning = true;

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final PrintWriter out;
        private final BufferedReader in;
        private final List<ClientHandler> clients;
        private final int clientId;
        private final AtomicInteger clientCount;

        public ClientHandler(Socket socket, List<ClientHandler> clients, int clientId, AtomicInteger clientCount) throws IOException {
            this.clientSocket = socket;
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.clients = clients;
            this.clientId = clientId;
            this.clientCount = clientCount;
        }

        @Override
        public void run() {
            try {
                String inputLine;
                broadcast("Client " + clientId + " has joined the chat");
                
                while ((inputLine = in.readLine()) != null) {
                    String message = "Client " + clientId + ": " + inputLine;
                    System.out.println(message);
                    broadcast(message);
                    
                    if ("exit".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void broadcast(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.out.println(message);
                    }
                }
            }
        }

        private void disconnect() {
            try {
                synchronized (clients) {
                    clients.remove(this);
                    clientCount.decrementAndGet();
                    broadcast("Client " + clientId + " has left the chat");
                }
                in.close();
                out.close();
                clientSocket.close();
                System.out.println("Client " + clientId + " disconnected. Total clients: " + clientCount.get());
            } catch (IOException e) {
                System.out.println("Error disconnecting client " + clientId + ": " + e.getMessage());
            }
        }

        public void sendMessage(String message) {
            out.println(message);
        }
    }

    public void start(int portNo) throws IOException {
        serverSocket = new ServerSocket(portNo);
        System.out.println("Server started on port " + portNo);

        // Thread for accepting new clients
        new Thread(() -> {
            while (isRunning) {
                try {
                    if (clientCount.get() < MAX_CLIENTS) {
                        Socket clientSocket = serverSocket.accept();
                        int clientId = clientCount.incrementAndGet();
                        System.out.println("New client connected. Total clients: " + clientId);
                        
                        ClientHandler clientHandler = new ClientHandler(clientSocket, clients, clientId, clientCount);
                        synchronized (clients) {
                            clients.add(clientHandler);
                        }
                        new Thread(clientHandler).start();
                    } else {
                        Socket rejectedSocket = serverSocket.accept();
                        PrintWriter rejectedOut = new PrintWriter(rejectedSocket.getOutputStream(), true);
                        rejectedOut.println("Server is full. Please try again later.");
                        rejectedSocket.close();
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.out.println("Error accepting client connection: " + e.getMessage());
                    }
                }
            }
        }).start();

        // Thread for server commands
        try (Scanner scanner = new Scanner(System.in)) {
            String command;
            while (true) {
                System.out.print("Server command (broadcast <message> or exit): ");
                command = scanner.nextLine();
                
                if (command.startsWith("broadcast ")) {
                    String message = "Server: " + command.substring(9);
                    synchronized (clients) {
                        for (ClientHandler client : clients) {
                            client.sendMessage(message);
                        }
                    }
                } else if ("exit".equalsIgnoreCase(command)) {
                    break;
                }
            }
        }

        stop();
    }

    public void stop() throws IOException {
        isRunning = false;
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.sendMessage("Server is shutting down...");
            }
            clients.clear();
        }
        serverSocket.close();
        System.out.println("Server stopped");
    }
}

public class MultipleServer {
    public static void main(String[] args) throws IOException {
        ChatServer chatServer = new ChatServer();
        chatServer.start(5000);
    }
}