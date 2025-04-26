import java.io.*;
import java.util.ArrayList;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPconnection {

    // Flags for which end of TCP this is
    private byte TCP_sender = 1;
    private byte TCP_receiver = 2;
    private byte TCPmode; // 1 for client, 2 for server

    // Basic status variables
    public boolean isOpen;
    public boolean isConnected;
    public boolean isClosed;

    // TCP connection variables
    private TCPtimeout timeout;
    private TCPfileHandling fileHandler;
    private int communicationPort; // Port that will be used for communication on this host
    private int targetPort; // Port at remote host that will be targetted
    private String targetIPAddress; // IP address to communicate to
    private int maxUnitSize; // Maximum Transmission Unit in bytes
    private int maxUnits; // Maximumliding window size in number of segments 

    // Communication variables
    private DatagramSocket socket_in; // Socket for communication in
    private DatagramSocket socket_out; // Socket for communication out

    // Message variables
    private ArrayList <TCPmessageStatus> messageListIn; // Buffer for messages in
    private ArrayList <TCPmessageStatus> messageListOut; // Buffer for messages out

    // Other Attributes
    private boolean extra_logging = true; // Change this flag based on level of logging needed
    private int maxBytes = 1518; // Maximum bytes to expect in a packet
    private int maxRetries = 16; // Maximum number of retries for sending a packet

    /**
     * This is for creating a new instance of TCPconnection for a client
     * @param fileHandler The instance of the file handler
     * @param target_ipAddress The IP address to communicate to
     * @param communicationPort The port to communicate on
     * @param targetPort The port to target
     * @param maxUnitSize The maximum unit size
     * @param maxUnits The maximum number of units
     */
    public TCPconnection(TCPfileHandling fileHandler, String target_ipAddress, int communicationPort, int targetPort, int maxUnitSize, int maxUnits) {
        this.TCPmode = 1;
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        this.fileHandler = fileHandler;
        this.targetPort = targetPort;
        this.targetIPAddress = target_ipAddress;
        this.messageListIn = new ArrayList <TCPmessageStatus>();
        this.messageListOut = new ArrayList <TCPmessageStatus>();
        this.timeout = new TCPtimeout();
    }

    /**
     * This is for creating a new instance of TCPconnection for a server
     * @param fileHandler The instance of the file handler
     * @param communicationPort The port to communicate on
     * @param maxUnitSize The maximum unit size
     * @param maxUnits The maximum number of units
     * 
     */
    public TCPconnection(TCPfileHandling fileHandler, int communicationPort, int maxUnitSize, int maxUnits) {
        this.TCPmode = 2;
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        this.fileHandler = fileHandler;
        this.communicationPort = communicationPort;
        this.maxUnitSize = maxUnitSize;
        this.maxUnits = maxUnits;
        this.messageListIn = new ArrayList <TCPmessageStatus>();
        this.messageListOut = new ArrayList <TCPmessageStatus>();
        this.timeout = new TCPtimeout();
    }


    /**
     * This is for after all initial validation is completed and the communication can be started.
     * @return True if successful, false if not
     */
    public boolean performTCPcommunication() {
        
        // Open the ports for communication and for listening
        this.socket_in = createSocket(communicationPort);
        this.socket_out = createSocket(0); // Use a random port for outgoing communication

        this.isOpen = true;
        this.isConnected = false;
        this.isClosed = false;

        // Establish the TCP connection
        if (this.TCPmode == TCP_sender) {
            // Client mode
            System.out.println("Establishing TCP connection to " + this.targetIPAddress + ":" + this.targetPort + "...");
            if (!clientEstablishTCPconnection()) {
                System.out.println("Failed to establish TCP connection in client mode.");
                return false;
            }
        } else if (this.TCPmode == TCP_receiver) {
            // Server mode
            System.out.println("Waiting for incoming TCP connection on port " + this.communicationPort + "...");
            if (!serverOpenListeningState()) {
                System.out.println("Failed to establish TCP connection in server mode.");
                return false;
            }
        }


        // Share data with over the TCP connection

        // Terminate the TCP connection
        
        
        // Close the sockets
        try {
            socket_in.close();
            socket_out.close();
        } catch (Exception e) {
            System.out.println("Error closing sockets: " + e.getMessage());
            return false;
        }
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        System.out.println("TCP connection closed.");
        
        return true; // Return true to indicate completion
    }

    /**************************************************** Server communication methods ****************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean serverOpenListeningState() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        DatagramPacket workingPacket = new DatagramPacket(buffer, buffer.length);
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
        int attempts = 0;

        // Listen for incoming packets for 60 seconds
        while (attempts < this.maxRetries) {
            
            // Wait for the client to send a SYN packet
            try {
                responsePacket = receivePacket(responsePacket); 
            } catch (Exception e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }

            // Check if the packet is null
            if (responsePacket == null) {
                attempts++;
                continue; // Retry sending the SYN-ACK packet
            }
            else {
                break; // Exit the loop if the ACK packet is received successfully
            }
        }

        // Check if the packet is null
        if (workingPacket == null) {
            System.out.println("No packet received within the timeout period of 60 seconds, closing the port and exiting.");
            return false;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCVinit = new TCPmessageStatus(workingPacket.getData());

        // Check if the packet is null
        if (tcpMessageRCVinit == null) {
            System.out.println("Error parsing TCP message, closing the port and exiting.");
            return false;
        }

        // Verify the packet is a new SYN packet
        if (tcpMessageRCVinit.verifyMessage(0, 0, 1, 0, 0) == false) {
            System.out.println("Received packet is not a SYN packet. Closing connection.");
            return false;
        }

        // Store the received packet in the message list
        this.messageListIn.add(tcpMessageRCVinit);

        // Process the received packet
        this.targetIPAddress = workingPacket.getAddress().getHostAddress();
        this.targetPort = workingPacket.getPort();

        // Create a new TCP message that is a SYN-ACK packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, tcpMessageRCVinit.byteSequenceNumber + 1);
        outTCP.setDatalessMessage(1, 0, 1); // SYN = 1, ACK = 1, FIN = 0
        this.messageListOut.add(outTCP);

        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        TCPmessageStatus tcpMessageRCVack = sendAndWaitForResponse(outTCP); 

        // Check if the packet is null
        if (tcpMessageRCVack == null) {
            System.out.println("No packet received within the timeout period of 30 seconds for acknowledgement, closing the port and exiting.");
            return false;
        }

        // Verify the packet is an ACK packet
        if (tcpMessageRCVack.verifyMessage(1, 1, 0, 1, 0) == false) {
            System.out.println("Received packet is not an ACK packet. Closing connection.");
            return false;
        }

        // Add the received packet to the message list
        this.messageListIn.add(tcpMessageRCVack);

        return true; // Return true to indicate success
    }


    /********************************************************* Client communication methods  ***************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean clientEstablishTCPconnection() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        int attempts = 0;

        //  Create a new TCP message that is a SYN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, 0);
        outTCP.setDatalessMessage(1, 0, 0); // SYN = 1, ACK = 0, FIN = 0
        this.messageListOut.add(outTCP);

        // Send the SYN packet to the server and listen for a response
        TCPmessageStatus tcpMessageRCVack = sendAndWaitForResponse(outTCP); 

        // Check if the packet is null
        if (tcpMessageRCVack == null) {
            System.out.println("No packet received within the timeout period for SYN-ACK, closing the port and exiting.");
            return false;
        }

        // Verify the packet is a SYN-ACK packet
        if (tcpMessageRCVack.verifyMessage(0, 1, 1, 0, 1) == false) {
            System.out.println("Received packet is not a SYN-ACK packet. Closing connection.");
            return false;
        }

        // Store the received packet in the message list
        this.messageListIn.add(tcpMessageRCVack);

        // Create a new TCP message that is a SYN-ACK packet
        TCPmessageStatus outTCP2 = new TCPmessageStatus(1, tcpMessageRCVack.byteSequenceNumber + 1);
        outTCP2.setDatalessMessage(0, 0, 1); // SYN = 0, FIN = 0, ACK = 1
        this.messageListOut.add(outTCP2);

        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        TCPmessageStatus tcpMessageRCVack2 = sendAndWaitForResponse(outTCP2); 

        // Check if the packet is null
        if (tcpMessageRCVack2 == null) {
            System.out.println("No packet received within the timeout period for acknowledgement, closing the port and exiting.");
            return false;
        }

        // Verify the packet is an ACK packet
        if (tcpMessageRCVack2.verifyMessage(1, 1, 0, 1, 0) == false) {
            System.out.println("Received packet is not an ACK packet. Closing connection.");
            return false;
        }

        // Add the received packet to the message list
        this.messageListIn.add(tcpMessageRCVack2);

        return true; // Return true to indicate success
    }



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
     */
    private DatagramPacket createPacket(byte[] data) {
        // Create a new DatagramPacket with the specified data and address
        try {
            return new DatagramPacket(data, data.length, InetAddress.getByName(this.targetIPAddress), this.targetPort);
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
                return new DatagramSocket(); // Use a random port, this one is outbound
            }

            DatagramSocket socket = new DatagramSocket(port);
            socket.setSoTimeout(1000); // Set a timeout for receiving packets

            // Create the socket with the specified port
            return socket;

        } catch (Exception e) {
            System.out.println("Error creating socket: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * This method is for the situation where you send a package and then wait for a response.
     * Passing null for the packet will just wait for a response.
     * @param tcpMessage
     * @param waitTime in milliseconds
     * @return packet
     * @throws IOException
     */
    private TCPmessageStatus sendAndWaitForResponse(TCPmessageStatus tcpMessage) {
        
        // Initialize
        int attempts = 0;
        DatagramPacket outPacket = null;

        // Create a new DatagramPacket to send the data if not void
        if (tcpMessage != null) {
            byte[] data = tcpMessage.getMessage();
            outPacket = createPacket(data);
        } 
        
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);

        while (attempts < this.maxRetries) {

            // Send a packet using the socket if there is one
            if (outPacket != null) {
                try {
                sendPacket(this.socket_out, outPacket, tcpMessage);
                } catch (IOException e) {
                    System.out.println("Error sending packet: " + e.getMessage());
                }
            }
            
            // Wait for the client to send a response packet
            try {
                responsePacket = receivePacket(responsePacket); 
            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
            }

            // Check if the packet is null
            if (responsePacket == null) {
                attempts++;
                continue; // Retry sending the SYN-ACK packet
            }
            else {
                break; // Exit the loop if the ACK packet is received successfully
            }
        }

        // Check if the packet is null
        if (responsePacket == null) {
            System.out.println("No packet received within the timeout period.");
            return null;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCV = new TCPmessageStatus(responsePacket.getData());

        // Return the received packet
        return tcpMessageRCV;
    }

    /**
     * This method will send a packet using the socket.
     * @param socket
     * @param packet
     * @param tcpMessage
     * @throws IOException
     */
    private void sendPacket(DatagramSocket socket, DatagramPacket packet, TCPmessageStatus tcpMessage) throws IOException {
        // Send the packet using the socket
        socket.send(packet);
        // Log the sent packet
        tcpMessage.sent = true;
        tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
    }

    /**
     * This method will listen for incoming packets on the socket.
     * @param packet
     * @param waitTime in nanoseconds
     * @return
     * @throws IOException
     */
    private DatagramPacket receivePacket(DatagramPacket packet) throws IOException {
        
        // Convert nanoseconds to milliseconds
        int waitTime = (int) (this.timeout.getTimeOut() / 1000000);

        // Set the timeOut for this listen
        socket_in.setSoTimeout(waitTime);

        // Receive the packet using the socket
        socket_in.receive(packet);
        DatagramPacket return_packet = new DatagramPacket(packet.getData(), packet.getLength());
        
        // Convert to TCPmessageStatus to print received message details
        TCPmessageStatus tcpMessage = new TCPmessageStatus(return_packet.getData());
        tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
        tcpMessage.received = true;
        
        // Return the packet
        return return_packet;
    }

    
}