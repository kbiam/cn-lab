import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MultipleServer {
    private static final int MAX_CLIENTS = 4;
    private static final int PORT = 9876;
    private static final Map<SocketAddress, ClientInfo> clients = new ConcurrentHashMap<>();
    private static volatile boolean running = true;

    static class ClientInfo {
        long lastMessageTime;
        int clientId;

        ClientInfo(int clientId) {
            this.clientId = clientId;
            this.lastMessageTime = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(PORT);
            byte[] receiveData = new byte[1024];
            byte[] sendData;

            System.out.println("Server started on port " + PORT);

            // Start client timeout checker thread
            startTimeoutChecker();

            // Start server command thread
            startServerCommandThread(serverSocket);

            while (running) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                
                try {
                    serverSocket.receive(receivePacket);
                    SocketAddress clientAddress = receivePacket.getSocketAddress();
                    String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                    // Handle new client connection
                    if (message.startsWith("CONNECT")) {
                        handleNewClient(serverSocket, clientAddress);
                        continue;
                    }

                    // Handle existing client message
                    if (clients.containsKey(clientAddress)) {
                        ClientInfo clientInfo = clients.get(clientAddress);
                        clientInfo.lastMessageTime = System.currentTimeMillis();

                        // Handle client disconnect
                        if (message.equals("EXIT")) {
                            handleClientDisconnect(serverSocket, clientAddress, clientInfo);
                            continue;
                        }

                        // Broadcast message to other clients
                        String broadcastMessage = "Client " + clientInfo.clientId + ": " + message;
                        System.out.println(broadcastMessage);
                        broadcastToOtherClients(serverSocket, clientAddress, broadcastMessage);
                    }
                } catch (Exception e) {
                    if (running) {
                        System.out.println("Error processing message: " + e.getMessage());
                    }
                }
            }

            // Cleanup
            for (Map.Entry<SocketAddress, ClientInfo> entry : clients.entrySet()) {
                sendMessage(serverSocket, entry.getKey(), "Server shutting down...");
            }
            serverSocket.close();
            System.out.println("Server stopped");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleNewClient(DatagramSocket serverSocket, SocketAddress clientAddress) {
        if (clients.size() >= MAX_CLIENTS) {
            sendMessage(serverSocket, clientAddress, "Server is full. Please try again later.");
            return;
        }

        if (!clients.containsKey(clientAddress)) {
            int clientId = clients.size() + 1;
            clients.put(clientAddress, new ClientInfo(clientId));
            sendMessage(serverSocket, clientAddress, "Connected as Client " + clientId);
            broadcastToOtherClients(serverSocket, clientAddress, "Client " + clientId + " has joined");
            System.out.println("New client connected. Total clients: " + clients.size());
        }
    }

    private static void handleClientDisconnect(DatagramSocket serverSocket, SocketAddress clientAddress, ClientInfo clientInfo) {
        clients.remove(clientAddress);
        broadcastToOtherClients(serverSocket, clientAddress, "Client " + clientInfo.clientId + " has left");
        System.out.println("Client " + clientInfo.clientId + " disconnected. Total clients: " + clients.size());
    }

    private static void broadcastToOtherClients(DatagramSocket serverSocket, SocketAddress senderAddress, String message) {
        for (Map.Entry<SocketAddress, ClientInfo> entry : clients.entrySet()) {
            if (!entry.getKey().equals(senderAddress)) {
                sendMessage(serverSocket, entry.getKey(), message);
            }
        }
    }

    private static void sendMessage(DatagramSocket serverSocket, SocketAddress address, String message) {
        try {
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address);
            serverSocket.send(sendPacket);
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    private static void startTimeoutChecker() {
        new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(30000); // Check every 30 seconds
                    long currentTime = System.currentTimeMillis();
                    
                    // Remove clients that haven't sent a message in 2 minutes
                    clients.entrySet().removeIf(entry -> 
                        (currentTime - entry.getValue().lastMessageTime) > 120000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private static void startServerCommandThread(DatagramSocket serverSocket) {
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (running) {
                System.out.print("Server command (broadcast <message> or exit): ");
                String command = scanner.nextLine();

                if (command.startsWith("broadcast ")) {
                    String message = "Server: " + command.substring(9);
                    for (SocketAddress clientAddress : clients.keySet()) {
                        sendMessage(serverSocket, clientAddress, message);
                    }
                } else if ("exit".equalsIgnoreCase(command)) {
                    running = false;
                    break;
                }
            }
            scanner.close();
        }).start();
    }
}