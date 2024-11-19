import java.net.*;

public class Client {
    public static void main(String[] args) {
        try {
            // Create a DatagramSocket for sending messages
            DatagramSocket clientSocket = new DatagramSocket();

            // Specify the server IP address and port number
            InetAddress IPAddress = InetAddress.getByName("localhost");
            byte[] sendData = new byte[1024];

            // Prepare the message to send
            String message = "Hello, UDP Server!";
            sendData = message.getBytes();

            // Create a packet with the message, server IP, and port number
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

            // Send the packet to the server
            clientSocket.send(sendPacket);

            // Close the socket
            clientSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
