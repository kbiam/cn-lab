import java.net.*;

public class Server {
    public static void main(String[] args) {
        try {
            // Create a DatagramSocket to receive messages
            DatagramSocket serverSocket = new DatagramSocket(9876);
            byte[] receiveData = new byte[1024];

            System.out.println("Server is waiting for messages...");

            // Create a packet to receive data
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            // Receive the packet from the client
            serverSocket.receive(receivePacket);
            String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

            System.out.println("Received from client: " + message);

            // Close the socket
            serverSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
