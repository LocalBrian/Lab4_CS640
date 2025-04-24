import java.io.*;
import java.nio.file.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPend {

    // Flags for which end of TCP this is
    private byte TCP_sender = 1;
    private byte TCP_receiver = 2;
    private byte tcp_type;

    // Arguments that can be passed in 
    private short listening_port; // Port that will be used for communication
    private short target_port; // Port at remote host that will be targetted
    private int target_ipAddress; // IP address to communicate to
    private String file_name; // Filename that will be used
    private int mtu; // Maximum Transmission Unit in bytes
    private int window_size; // Sliding window size in number of segments

    // Other Attributes
    private boolean extra_logging = true; // Change this flag based on level of logging needed
    private DatagramSocket socket_in; // Socket for communication in
    private DatagramSocket socket_out; // Socket for communication out

    /************************** Start Main ****************************************/

    public static void main(String[] args) {
        System.out.println("Executing TCPend on host.");

        // Create an instance of TCPend
        TCPend tcpE = new TCPend();

        // Verify there was an appropriate amount of arguments sent
        if (args.length != 12) {
            if (args.length != 8) {
                System.out.println("An incorrect number of arguements was sent for a sender or receiver.");
                return;
            }
        }

        // Parse the arguments
        tcpE.parseArgs(args);

        // Create the sockets for sending and receiving data
        tcpE.socket_out = tcpE.createSocket(0); // Use a random port for sending
        if (tcpE.socket_out == null) {
            System.out.println("Error creating socket for sending data.");
            return;
        }
        tcpE.socket_in = tcpE.createSocket(tcpE.listening_port);
        if (tcpE.socket_in == null) {
            System.out.println("Error creating socket for receiving data.");
            return;
        }
        

        // Create the datagram packet for sending data, data should be small and printed at destination
        if (tcpE.tcp_type == tcpE.TCP_sender) {

            System.out.println("Sender mode is active.");

            // Create File Chunk Reader
            TCPfilehandling fileChunkReader = new TCPfilehandling(tcpE.file_name, tcpE.mtu);
            byte[] chunk;

            // Verify file exists
            if (!fileChunkReader.startupVerifyFolderFile(false)) {
                System.out.println("File does not exist.");
                return;
            }

            while (fileChunkReader.hasNextChunk()) {
                // Read the next chunk of data from the file
                try {
                    chunk = fileChunkReader.readNextChunk();
                } catch (IOException e) {
                    System.out.println("Error reading file: " + e.getMessage());
                    return;
                }
                // Create full TCP packet and include header 
                TCPheader tcpHeader = new TCPheader(0, 0, System.nanoTime(), chunk.length, 1, 0, 0, chunk);
                tcpHeader.parseReceivedTCP(tcpHeader.returnFullHeader());

                // Create a packet with the chunk data
                DatagramPacket packet = tcpE.createPacket(tcpHeader.returnFullHeader(), 0, tcpHeader.returnFullHeader().length, tcpE.target_ipAddress, tcpE.target_port);
                if (packet == null) {
                    System.out.println("Error creating packet.");
                    return;
                }

                // Send the packet
                try {
                    tcpE.sendPacket(tcpE.socket_out, packet);
                    System.out.println("Packet sent successfully.");
                } catch (IOException e) {
                    System.out.println("Error sending packet: " + e.getMessage());
                    return;
                }
                // Sleep for 1 second to simulate network delay
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("Error sleeping: " + e.getMessage());
                    return;
                }
            }    

        } else {
            long endTime = System.currentTimeMillis() + 10000; // Set the end time to ten seconds from now
            TCPfilehandling fileChunkWriter = new TCPfilehandling(tcpE.file_name);
            // Verify file exists
            if (!fileChunkWriter.startupVerifyFolderFile(true)) {
                System.out.println("File does not exist.");
                return;
            }
            // Keep looping received mode for ten seconds
            while (System.currentTimeMillis() < endTime) {
                // Sleep for a short duration to avoid busy waiting
                System.out.println("Receiver mode is active.");
                byte[] buffer = new byte[1024]; // Buffer for receiving data
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    packet = tcpE.receivePacket(tcpE.socket_in, buffer, buffer.length);
                } catch (IOException e) {
                    System.out.println("Error receiving packet: " + e.getMessage());
                    return;
                }
                // Parse the TCP data
                TCPheader tcpHeader = new TCPheader();
                tcpHeader.parseReceivedTCP(packet.getData());
                // Process the received packet
                try {
                fileChunkWriter.writeByteArrayToFile(tcpHeader.data, tcpHeader.dataLength, 0);
                } catch (IOException e) {
                    System.out.println("Error writing to file: " + e.getMessage());
                    return;
                }

                // Print the received data
                // System.out.println("Packet received successfully.");
                // System.out.println("Data: " + tcpE.byteArrayToString(packet.getData()));
                // System.out.println("Sender IP: " + packet.getAddress().getHostAddress());
                // System.out.println("Sender Port: " + packet.getPort());
                // System.out.println("Data Length: " + packet.getLength());
            }    
        }
        
        // Close the sockets
        tcpE.closeSocket(tcpE.socket_in);
        tcpE.closeSocket(tcpE.socket_out);
        System.out.println("Sockets closed successfully.");
        System.out.println("TCPend execution completed.");
    }

    /********************************* Code to handle the TCP Initiation. **************************************************/

    /** 
     * This method is for initializing TCP as a sender
     */

    /**
     * This method is for initializing TCP as a receiver
     */

    /****************** This code will handle construction of the datagram into properly receivable data including the header. ********************************/

    /**
     * This method will take a byte array and convert it to a string.
     * Don't need inverse, file chunk reader generates byte array.
     * @param data
     * @return
     */
    private String byteArrayToString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append((char) b);
        }
        return sb.toString();
    }

    /**
     * This method will create a packet for sending data.
     * @param data
     * @param offset
     * @param length
     * @param address
     * @param port
     */
    private DatagramPacket createPacket(byte[] data, int offset, int length, int address, int port) {
        // Create a new DatagramPacket with the specified data and address
        try {
            return new DatagramPacket(data, offset, length, InetAddress.getByName(TCPStringfromIPv4Address(address)), port);
        } catch (UnknownHostException e) {
            System.out.println("Error creating packet: " + e.getMessage());
            return null;
        }
        
    }

    /******************************** This is the code that will handle sending of packets and creating sockets. *******************************************/
    /**
     * This method will create a socket for sending data.
     * @param port if 0 will assign random port, otherwise attempt with assigned value
     * @return
     */
    private DatagramSocket createSocket(int port) {
        try {
            // Create a new DatagramSocket a random port if port = 0
            if (port == 0) {
                return new DatagramSocket(); // Use a random port
            }
            // Create the socket with the specified port
            return new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Error creating socket: " + e.getMessage());
            return null;
        }
    }
    

    /**
     * This method will send a packet using the socket.
     * @param socket
     * @param packet
     * @throws IOException
     */
    private void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        // Send the packet using the socket
        socket.send(packet);
    }

    /**
     * This method will close the socket.
     * @param socket
     */
    private void closeSocket(DatagramSocket socket) {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * This method will listen for incoming packets on the socket.
     * @param socket
     * @param buffer
     * @param length
     * @return
     * @throws IOException
     */
    private DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer, int length) throws IOException {
        // Create a new DatagramPacket to receive the data
        DatagramPacket packet = new DatagramPacket(buffer, length);
        // Receive the packet using the socket
        socket.receive(packet);
        DatagramPacket return_packet = new DatagramPacket(packet.getData(), packet.getLength());
        return return_packet;
    }

    /************************************ Code to handle initial startup and parsing of arguments. *********************************************/
    private void parseArgs(String[] args) {
        
        // Default to receiver, will update if argument for sender is received
        tcp_type = TCP_receiver;

        // Parse arguments
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.equals("-p"))
			{ listening_port = Short.parseShort(args[++i]); }
			else if(arg.equals("-s"))
            { target_ipAddress = TCPtoIPv4Address(args[++i]);
              tcp_type = TCP_sender; }
			else if (arg.equals("-a"))
			{ target_port = Short.parseShort(args[++i]); }
			else if (arg.equals("-f"))
			{ file_name = args[++i]; }
			else if (arg.equals("-m"))
			{ mtu = Integer.parseInt(args[++i]); }
			else if (arg.equals("-c"))
			{ window_size = Integer.parseInt(args[++i]); }
		}

        // Verify that the needed arguments for Listener or sender were given.
        if (tcp_type == TCP_sender) {
            if (listening_port == 0 || target_ipAddress == 0 || target_port == 0 || file_name == null || mtu == 0 || window_size == 0) {
                System.out.println("An argument was missing for the sender.");
                return;
            }
        } else {
            if (listening_port == 0 || file_name == null || mtu == 0 || window_size == 0) {
                System.out.println("An argument was missing for the receiver.");
                return;
            }
        }

        // If extra logging then print details passed in
        if (extra_logging) {
            System.out.println("TCPend: TCP Type: " + (tcp_type == TCP_sender ? "Sender" : "Receiver"));
            System.out.println("TCPend: Listening Port: " + listening_port);
            System.out.println("TCPend: Target Port: " + target_port);
            System.out.println("TCPend: Target IP Address: " + TCPStringfromIPv4Address(target_ipAddress));
            System.out.println("TCPend: File Name: " + file_name);
            System.out.println("TCPend: MTU: " + mtu);
            System.out.println("TCPend: Window Size: " + window_size);
        }

    }

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     * Code is taken from IPv4.java, from class.
     * @param ipAddress
     * @return
     */
    public int TCPtoIPv4Address(String ipAddress) {
        if (ipAddress == null)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) 
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result |= Integer.valueOf(octets[i]) << ((3-i)*8);
        }
        return result;
    }

    /** 
     * Accepts an IPv4 address and returns of string of the form xxx.xxx.xxx.xxx
     * ie 192.168.0.1
     * Code is taken from IPv4.java, from class.
     * 
     * @param ipAddress
     * @return
     */
    public static String TCPStringfromIPv4Address(int ipAddress) {
        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3-i)*8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3)
                sb.append(".");
        }
        return sb.toString();
    }

      

}
