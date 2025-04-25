import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class TCPconnection {

    // Basic status variables
    public boolean isOpen;
    public boolean isConnected;
    public boolean isClosed;
    private int TCPmode; // 1 for client, 2 for server

    // TCP connection variables
    private TCPtimeout timeout;
    private TCPfileHandler fileHandler;
    private short communicationPort; // Port that will be used for communication on this host
    private short targetPort; // Port at remote host that will be targetted
    private int targetIPAddress; // IP address to communicate to
    private int maxUnitSize; // Maximum Transmission Unit in bytes
    private int maxUnits; // Maximumliding window size in number of segments 

    // Communication variables
    private DatagramSocket socket_in; // Socket for communication in
    private DatagramSocket socket_out; // Socket for communication out

    // Message variables
    private TCPmessageStatus[] messageListIn; // Buffer for messages in
    private TCPmessageStatus[] messageListOut; // Buffer for messages out

    // Other Attributes
    private boolean extra_logging = true; // Change this flag based on level of logging needed
    private int timeoutStartseconds = 5; // Timeout start point in seconds
    private int maxBytes = 1518; // Maximum bytes to expect in a packet

    /**
     * This is for creating a new instance of TCPconnection for a client
     * @param fileHandler The instance of the file handler
     * @param target_ipAddress The IP address to communicate to
     * @param communicationPort The port to communicate on
     * @param targetPort The port to target
     * @param maxUnitSize The maximum unit size
     * @param maxUnits The maximum number of units
     */
    public boolean TCPconnection(TCPfileHandler fileHandler, int target_ipAddress, short communicationPort, short targetPort, int maxUnitSize, int maxUnits) {
        this.TCPmode = 1;
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        this.fileHandler = fileHandler;
        this.targetPort = targetPort;
        this.targetIPAddress = target_ipAddress;
        this.messageListIn = new TCPmessageStatus[];
        this.messageListOut = new TCPmessageStatus[];
        return true;
    }

    /**
     * This is for creating a new instance of TCPconnection for a server
     * @param fileHandler The instance of the file handler
     * @param communicationPort The port to communicate on
     * @param maxUnitSize The maximum unit size
     * @param maxUnits The maximum number of units
     * 
     */
    public boolean TCPconnection(TCPfileHandler fileHandler, short communicationPort, int maxUnitSize, int maxUnits) {
        this.TCPmode = 2;
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        this.fileHandler = fileHandler;
        this.communicationPort = communicationPort;
        this.maxUnitSize = maxUnitSize;
        this.maxUnits = maxUnits;
        this.messageListIn = new TCPmessageStatus[];
        this.messageListOut = new TCPmessageStatus[];
        return true;
    }


    /**
     * This is for after all initial validation is completed and the communication can be started.
     * @return True if successful, false if not
     */
    public boolean startTCPcommunication() {
        
        // Open the ports for communication and for listening
        try {
            this.socket_in = createSocket(communicationPort);
            this.socket_out = createSocket(0); // Use a random port for outgoing communication
        } catch (SocketException e) {
            System.out.println("Error creating sockets: " + e.getMessage());
            return false;
        }
        this.isOpen = true;
        this.isConnected = false;
        this.isClosed = false;

        // Establish the TCP connection


        // Share data with over the TCP connection

        // Terminate the TCP connection

        
        return true; // Return true to indicate success
    }

    /**************************************************** Server communication methods ****************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean serverListeningState() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        DatagramPacket workingPacket = new DatagramPacket(buffer, buffer.length);

        try {
            workingPacket = receivePacket(workingPacket, 30000); // 30 seconds timeout
        } catch (Exception e) {
            System.out.println("Error receiving packet: " + e.getMessage());
            return false;
        }

        // Check if the packet is null
        if (workingPacket == null) {
            System.out.println("No packet received within the timeout period of 30 seconds.");
            return false;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCVinit = new TCPmessageStatus(workingPacket.getData());

        // Verify this is a SYN packet
        if (tcpMessageRCVinit.SYN == 0) {
            System.out.println("Received packet is not a SYN packet. Closing connection.");
            return false;
        }
        this.messageListIn.add(tcpMessageRCVinit);

        // Process the received packet
        this.targetIPAddress = workingPacket.getAddress().getHostAddress();
        this.targetPort = workingPacket.getPort();
        

        // Create a new TCP message that is a SYN-ACK packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, tcpMessageRCVinit.byteSequenceNumber + 1);
        byte[] data = outTCP.getDatalessMessage(1, 0, 1);
        this.messageListOut.add(outTCP);

        // Create a new DatagramPacket with the data
        DatagramPacket packet = createPacket(data, 0, data.length);

        // Send a response back to the client
        try {
            sendPacket(socket_out, packet);
        } catch (Exception e) {
            System.out.println("Error sending packet: " + e.getMessage());
            return false;
        }

        // Wait for the client to send an ACK packet
        try {
            workingPacket = receivePacket(workingPacket, 15000); // 15 seconds timeout
        } catch (Exception e) {
            System.out.println("Error receiving packet: " + e.getMessage());
            return false;
        }

        // Check if the packet is null
        if (workingPacket == null) {
            System.out.println("No packet received within the timeout period of 15 seconds.");
            return false;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCVack = new TCPmessageStatus(workingPacket.getData());

        // The 3 things below need to be their own method within TCPmessageStatus ------------------------------***

        // Verify this is an ACK packet
        if (tcpMessageRCVack.ACK == 0) {
            System.out.println("Received packet is not an ACK packet. Closing connection.");
            return false;
        }
        // Verify the acknowledgment number is correct
        if (tcpMessageRCVack.acknowledgmentNumber != tcpMessageRCVinit.byteSequenceNumber + 1) {
            System.out.println("Received packet has an incorrect acknowledgment number. Closing connection.");
            return false;
        }
        // Verify the sequence number is correct
        if (tcpMessageRCVack.byteSequenceNumber != tcpMessageRCVinit.acknowledgmentNumber) {
            System.out.println("Received packet has an incorrect sequence number. Closing connection.");
            return false;
        }
        
        // Add the received packet to the message list
        this.messageListIn.add(tcpMessageRCVack);

        return true; // Return true to indicate success
    }


    /********************************************************* Client communication methods  ***************************************************/



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
    private DatagramPacket createPacket(byte[] data, int offset, int length) {
        // Create a new DatagramPacket with the specified data and address
        try {
            return new DatagramPacket(data, offset, length, InetAddress.getByName(TCPStringfromIPv4Address(this.target_ipAddress)), this.targetPort);
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
     * @param packet
     * @param waitTime in milliseconds
     * @return
     * @throws IOException
     */
    private DatagramPacket receivePacket(DatagramPacket packet, int waitTime) throws IOException {
        
        // Set the timeOut for this listen
        socket_in.setSoTimeout(waitTime);

        // Receive the packet using the socket
        socket_in.receive(packet);
        DatagramPacket return_packet = new DatagramPacket(packet.getData(), packet.getLength());
        return return_packet;
    }

    
}