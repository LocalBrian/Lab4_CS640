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
    private TCPdataTracker dataTracker;
    private int communicationPort; // Port that will be used for communication on this host
    private int targetPort; // Port at remote host that will be targetted
    private InetAddress targetIPAddress; // IP address to communicate to
    private int maxUnitSize; // Maximum Transmission Unit in bytes
    private int maxUnits; // Maximumliding window size in number of segments 
    private int finBytSeqNum; // Byte sequence number of the FIN packet

    // Communication variables
    private DatagramSocket socket; // Socket for communication

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
    public TCPconnection(TCPfileHandling fileHandler, String string_ipAddress, int communicationPort, int targetPort, int maxUnitSize, int maxUnits) {
        this.TCPmode = 1;
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        this.fileHandler = fileHandler;
        this.targetPort = targetPort;
        this.maxUnits = 10;
        try {
            this.targetIPAddress = InetAddress.getByName(string_ipAddress);
        } catch (UnknownHostException e) {
            System.out.println("Error: " + e.getMessage());
            this.targetIPAddress = null;
        }
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
        this.socket = createSocket(communicationPort);

        this.isOpen = true;
        this.isConnected = false;
        this.isClosed = false;

        // Establish the TCP connection
        if (this.TCPmode == TCP_sender) {
            // Set the socket timer to 80 milliseconds
            try {
                this.socket.setSoTimeout(250);
            } catch (SocketException e) {
                System.out.println("Error setting socket timeout: " + e.getMessage());
            }
            // Client mode
            System.out.println("Establishing TCP connection to " + this.targetIPAddress + ":" + this.targetPort + "...");
            if (!clientEstablishTCPconnection()) {
                System.out.println("Failed to establish TCP connection in client mode.");
                this.endTCPcommunication();
                return false;
            }

            // Create the data tracker
            this.dataTracker = new TCPdataTracker(true, this.maxUnitSize, this.maxUnits, this.fileHandler);

        } else if (this.TCPmode == TCP_receiver) {
            // Set the socket timer to 50 milliseconds
            try {
                this.socket.setSoTimeout(200);
            } catch (SocketException e) {
                System.out.println("Error setting socket timeout: " + e.getMessage());
            }
            // Server mode
            System.out.println("Waiting for incoming TCP connection on port " + this.communicationPort + "...");
            if (!serverOpenListeningState()) {
                System.out.println("Failed to establish TCP connection in server mode.");
                this.endTCPcommunication();
                return false;
            }

            // Create the data tracker
            this.dataTracker = new TCPdataTracker(false, this.maxUnitSize, this.maxUnits, this.fileHandler);
        }

        // Share data over the TCP connection
        if (this.TCPmode == TCP_sender) {
            // Client mode
            System.out.println("Sending data...");
            if (!clientSendData()) {
                System.out.println("The client has lost connection.");
                this.endTCPcommunication();
                return false;
            }
        } else if (this.TCPmode == TCP_receiver) {
            // Server mode
            System.out.println("Receiving data...");
            if (!serverReceiveData()) {
                System.out.println("The server has lost connection.");
                this.endTCPcommunication();
                return false;
            }
        }

        // Communicate TCP connection end
        System.out.println("Closing TCP connection...");
        if (this.TCPmode == TCP_sender) {
            // Client mode
            if (clientCloseTCPconnection()) {
                System.out.println("Client closed TCP connection successfully.");
            } else {
                System.out.println("Failed to close TCP connection in client mode.");
                this.endTCPcommunication();
                return false;
            }
        } else if (this.TCPmode == TCP_receiver) {
            // Server mode
            if (serverCloseTCPconnection()) {
                System.out.println("Server closed TCP connection successfully.");
            } else {
                System.out.println("Failed to close TCP connection in server mode.");
                this.endTCPcommunication();
                return false;
            }
        }

        // Terminate the TCP connection        
        this.endTCPcommunication();
        
        return true; // Return true to indicate completion
    }

    /**
     * This method will end commmunication and close the socket.
     * 
     */
    public void endTCPcommunication() {
        
        // Close the sockets
        try {
            this.socket.close();
        } catch (Exception e) {
            System.out.println("Error closing sockets: " + e.getMessage());
        }
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;
        System.out.println("TCP connection closed.");
    }

    /**************************************************** Server communication methods ****************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean serverOpenListeningState() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        // DatagramPacket workingPacket = new DatagramPacket(buffer, buffer.length);
        DatagramPacket responsePacket = null;
        TCPmessageStatus tcpMessageRCVack = null;
        int attempts = 0;

        // Listen for incoming packets for 60 seconds
        while (attempts < this.maxRetries) {
            
            // Wait for the client to send a SYN packet
            try {
                responsePacket = receivePacket(); 
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
        if (responsePacket == null) {
            return false;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCVinit = new TCPmessageStatus(responsePacket.getData());

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
        this.targetIPAddress = responsePacket.getAddress();
        this.targetPort = responsePacket.getPort();
        System.out.println("Sending packet to :" + this.targetPort);

        // Create a new TCP message that is a SYN-ACK packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, tcpMessageRCVinit.byteSequenceNumber + 1);
        outTCP.setDatalessMessage(1, 0, 1); // SYN = 1, ACK = 1, FIN = 0
        this.messageListOut.add(outTCP);

        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        attempts = 0;
        while (attempts < this.maxRetries) {

        tcpMessageRCVack = sendAndWaitForResponse(outTCP, true); 

        // Check if the packet is null
        if (tcpMessageRCVack == null) {
            // Do nothing, just retry
            attempts++;
            } // Verify the packet is an ACK packet
            else if (tcpMessageRCVack.verifyMessage(1, 1, 0, 0, 1) == false) {
                // Do nothing, just retry
                attempts++;
            } else {
                break; // Exit the loop if the ACK packet is received successfully
            }        
        
        }
        
        // Check if the packet is null
        if (tcpMessageRCVack == null) {
            System.out.println("No packet received within the timeout period for SYN-ACK, closing the port and exiting.");
            return false;
        }

        // Add the received packet to the message list
        this.messageListIn.add(tcpMessageRCVack);

        return true; // Return true to indicate success
    }

    /**
     * This method is for when the server is receiving data from the client.
     */
    public boolean serverReceiveData() {

        // Initialize the variables for controlling the while loop
        boolean connectionLost = false;
        TCPmessageStatus inTCP = null;
        TCPmessageStatus outTCP = null;
        int attempts = 0;
        int position;
        this.finBytSeqNum = 1;

        // Loop until either a null or a FIN packet is received
        while (connectionLost == false) {

            this.messageListIn = new ArrayList <TCPmessageStatus>();

            while (attempts < this.maxRetries) {
                
                // Loop gathering packets sent
                while (true) {
                    // Wait for a packet to come in
                    System.out.println("Waiting for packet...");
                    // Wait for the client to send a packet
                    inTCP = sendAndWaitForResponse(null, true);
                    if (inTCP == null) {
                        break;
                    }
                    System.out.println("Received packet.");
                    // Check if the packet has previously been received
                    if (this.dataTracker.isDataReceived(inTCP.byteSequenceNumber)) {
                        System.out.println("Packet already received, skipping.");
                        // Send an ACK packet back to the client
                        outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                        outTCP.setDatalessMessage(0, 0, 1); // SYN = 0, ACK = 1, FIN = 0
                        this.sendAndWaitForResponse(outTCP, false);
                        continue; // Skip to the next packet
                    }
                    // Check if there is space in message list in
                    else if (this.messageListIn.size() == 0) {
                        System.out.println("First packet received in round, storing packet.");
                        this.messageListIn.add(inTCP);
                    }
                    // Check if there is space in message list in
                    else if (this.messageListIn.size() < this.maxUnits) {
                        System.out.println("Space for the data, storing packet.");
                        position = 0;
                        while (position < this.messageListIn.size()) {
                            // Check if the packet is already in the list
                            if (inTCP.byteSequenceNumber == this.messageListIn.get(position).byteSequenceNumber) {
                                System.out.println("Packet already in list, skipping.");
                                outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                                outTCP.setDatalessMessage(0, 0, 1); // SYN = 0, ACK = 1, FIN = 0
                                this.sendAndWaitForResponse(outTCP, false);
                                break; // Skip to the next message
                            } else if (inTCP.byteSequenceNumber < this.messageListIn.get(position).byteSequenceNumber) {
                                System.out.println("Packet is before the current packet, inserting.");
                                this.messageListIn.add(position, inTCP);
                                break; // Exit the loop if the packet is inserted
                            } else {
                                position++;
                            }
                        }
                        // If the packet is not inserted, add it to the end of the list
                        if (position == this.messageListIn.size()) {
                            System.out.println("Packet is highest byte sequence number, adding to end of list.");
                            this.messageListIn.add(inTCP);
                        }
                    } else {
                        System.out.println("Storage full, seeing if will be inserted.");
                        position = 0;
                        while (position < this.messageListIn.size()) {
                            // Check if the packet is already in the list
                            if (inTCP.byteSequenceNumber == this.messageListIn.get(position).byteSequenceNumber) {
                                System.out.println("Packet already in list, skipping.");
                                outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                                outTCP.setDatalessMessage(0, 0, 1); // SYN = 0, ACK = 1, FIN = 0
                                this.sendAndWaitForResponse(outTCP, false);
                                break; // Skip to the next message
                            } else if (inTCP.byteSequenceNumber < this.messageListIn.get(position).byteSequenceNumber) {
                                System.out.println("Packet is before the current packet, inserting and popping back item.");
                                this.messageListIn.remove(this.messageListIn.size() - 1); // Remove the last item
                                this.messageListIn.add(position, inTCP);
                                break; // Exit the loop if the packet is inserted
                            } else {
                                position++;
                            }
                        }
                        // Drop the packet
                        System.out.println("Packet is after the biggest packet or already received, dropping.");
                    }

                    // Any packets that are past the max units will be dropped
                }
                
                // Check if the list in is empty
                if (this.messageListIn.isEmpty() == true) {
                    System.out.println("No packets received, retrying...");
                    attempts++;
                    break; // Retry sending the SYN-ACK packet
                } // Check if there is a single FIN packet
                else if (this.messageListIn.size() == 1) {
                    if(this.messageListIn.get(0).verifyMessage(this.dataTracker.getNextExpectedByte(), 1, 0, 1, 0) == true) {
                    System.out.println("Received FIN packet. Initiating close.");
                    return true;
                    }
                }
                // Loop over the messages that were received
                while (this.messageListIn.size() > 0) {
                    // Check if the first packet has the next expected byte
                    inTCP = this.messageListIn.get(0);
                    if (inTCP.verifyMessage(this.dataTracker.getNextExpectedByte(), 1, 0, 0, 1) == true) {
                        System.out.println("Received next expectecd packet, processing.");
                        // Process the received data bytes
                        this.dataTracker.receiverAddData(inTCP.byteSequenceNumber, inTCP.dataLength, inTCP.getMessage());
                        // Send an ACK packet back to the client
                        outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                        outTCP.setDatalessMessage(0, 0, 1); // SYN = 0, ACK = 1, FIN = 0
                        this.sendAndWaitForResponse(outTCP, false);
                        // Remove the processed packet from the list
                        this.messageListIn.remove(0);
                    } else {
                        // The first packet isn't what we want - listen for more packets
                        System.out.println("First packet is not the next expected byte sequence number, checking for more packets.");
                        break;
                    }
                    
                
                }

            }            

            if (attempts == this.maxRetries) {
                System.out.println("No packet received within the timeout period for data, closing the port and exiting.");
                connectionLost = true;
            }

        }

        return false; // Return false to indicate connection was lost
    }

    // Close out communication for the server
    public boolean serverCloseTCPconnection() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        TCPmessageStatus inTCP = null;
        int attempts = 0;

        //  Create a new TCP message that is a FIN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(1, this.dataTracker.getNextExpectedByte() +1);
        outTCP.setDatalessMessage(0, 1, 1); // SYN = 0, ACK = 0, FIN = 1
        this.messageListOut.add(outTCP);

        while (attempts < this.maxRetries) {

            // Send the FIN packet to the server and listen for a response
            inTCP = sendAndWaitForResponse(outTCP, true); 

            // Check if the packet is null
            if (inTCP == null) {
                continue;
            } // Verify the packet is an SYN-ACK packet
            else if (inTCP.verifyMessage(this.dataTracker.getNextExpectedByte() +1, 2, 0, 0, 1) == true) {
                System.out.println("Received packet is an FIN-ACK packet. Closing connection.");
                break;
            }
        }

        // Store the received packet in the message list
        this.messageListIn.add(inTCP);

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
        TCPmessageStatus tcpMessageRCVack = sendAndWaitForResponse(outTCP, true); 

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

        // Create a new TCP message that is an ACK packet
        TCPmessageStatus outTCP2 = new TCPmessageStatus(1, tcpMessageRCVack.byteSequenceNumber + 1);
        outTCP2.setDatalessMessage(0, 0, 1); // SYN = 0, FIN = 0, ACK = 1
        this.messageListOut.add(outTCP2);

        // Send the ack packet and don't wait for a response
        TCPmessageStatus tcpMessageRCVack2 = sendAndWaitForResponse(outTCP2, false); 

        return true; // Return true to indicate success
    }

    /**
     * This method is for when the client is sending data to the server.
     */
    public boolean clientSendData() {
        // Initialize the variables for controlling the while loop
        boolean connectionLost = false;
        int attempts = 0;
        int count;
        TCPmessageStatus tcpMessageData = new TCPmessageStatus(0, 0);
        TCPmessageStatus tcpMessageRCVack = new TCPmessageStatus(0, 0);
        TCPmessageStatus lastSuccMess = null;
        byte[] data = null;
        int currentWindow = 1;
        int currentByteSqnNumber = 1;
        boolean finalRound = false;
        int activeMessagesAcked;
        

        // Loop until either a null or a FIN packet is received
        while (connectionLost == false && finalRound == false) {

            attempts = 0;
            count = 0;
            activeMessagesAcked = 0;

            // Build list of messages to be sent in a wave
            this.messageListOut = new ArrayList <TCPmessageStatus>();

            while (count < currentWindow) {
                // Create a new TCP message that is a data packet
                tcpMessageData = new TCPmessageStatus(0, 0);
                data = this.dataTracker.senderRetrieveData();
                // Check if the data is null, if so move to close connection
                if (data == null) {
                    finalRound = true;
                    this.finBytSeqNum = currentByteSqnNumber;
                    break; // Exit the loop if the ACK data packet is received successfully
                }
                tcpMessageData.setDataMessage(currentByteSqnNumber, 1, data);
                this.messageListOut.add(tcpMessageData);
                count++;
                currentByteSqnNumber += data.length;
            }

            // Total messages in this round
            System.out.println("Total messages in this round: " + this.messageListOut.size());

            while (attempts < this.maxRetries) {

                System.out.println("Attempt number: " + attempts);
                
                // Loop over the messages to be sent
                for (TCPmessageStatus message : this.messageListOut) {
                    System.out.println("Checking message: " + message.byteSequenceNumber);
                    // Send the packet using the socket
                    if (message.acknowledged == false) {
                        // Send the packet using the socket
                        tcpMessageRCVack = sendAndWaitForResponse(message, false);
                        System.out.println("Sent message: " + message.byteSequenceNumber);
                    }
                    
                }

                // While loop waiting for ACK
                while (true) {
                    // Wait for a packet to come in
                    tcpMessageRCVack = sendAndWaitForResponse(null, true);

                    // If the packet is null, break out of this loop
                    if (tcpMessageRCVack == null) {
                        System.out.println("Breaking out of listening loop, no packet received.");
                        break;
                    } 
                    // See if it matches one of the messages we sent
                    for (TCPmessageStatus message : this.messageListOut) {
                        System.out.println("Checking message: " + message.byteSequenceNumber + message.dataLength);
                        if (tcpMessageRCVack.verifyMessage( 1, message.byteSequenceNumber + message.dataLength, 0, 0, 1) == true) {
                            // Check if the message is already acknowledged
                            if (message.isAcknowledged() == true) {
                                System.out.println("Message already acknowledged, skipping.");
                                continue; // Skip to the next message
                            } // Increment the active messages acknowledged and mark the message as acknowledged
                            else {
                                System.out.println("Message acknowledged: " + message.byteSequenceNumber);
                                message.setAcknowledged();
                                activeMessagesAcked++;
                            }
                            
                        }

                    }
                    System.out.println("Active messages acknowledged: " + activeMessagesAcked);
                    System.out.println("Total messages sent: " + this.messageListOut.size());
                    if (activeMessagesAcked == this.messageListOut.size()) {
                        System.out.println("All messages acknowledged.");
                        break; // Exit the loop if all messages are acknowledged
                    }
                    else {
                        System.out.println("Not all messages acknowledged, waiting for more packets.");
                    }
                }

                System.out.println("Exited message loop check");

                // Check if active messages acknowledged is equal to the number of messages sent
                if (activeMessagesAcked == this.messageListOut.size()) {
                    // If the first attempt, then increase the window size
                    if (attempts == 0) {
                        currentWindow = Math.min(currentWindow *2, this.maxUnits);
                    } else {
                        currentWindow = Math.min(currentWindow /2, this.maxUnits);
                        currentWindow = Math.max(currentWindow, 1);
                    }
                    // All messages have been acknowledged, break out of the loop
                    break;
                }

                attempts++;

            }

            }
            if (attempts == this.maxRetries) {
                System.out.println("No packet received within the timeout period for data, closing the port and exiting.");
                connectionLost = true;
        }
        if (finalRound == true) {
            System.out.println("Final round of data sent.");
            return true; // Return true to indicate success
        }
        else if (connectionLost == true) {
            System.out.println("Connection lost, closing the port and exiting.");
            this.endTCPcommunication();
            return false; // Return false to indicate connection was lost
        } 
        return false; // Return false to indicate connection was lost
    }

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean clientCloseTCPconnection() {
        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        TCPmessageStatus inTCP = null;
        int attempts = 0;

        //  Create a new TCP message that is a FIN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(this.finBytSeqNum, 1);
        outTCP.setDatalessMessage(0, 1, 0); // SYN = 0, ACK = 0, FIN = 1
        this.messageListOut.add(outTCP);

        while (attempts < this.maxRetries) {

            // Send the FIN packet to the server and listen for a response
            inTCP = sendAndWaitForResponse(outTCP, true); 

            // Check if the packet is null
            if (inTCP == null) {
                continue;
            } // Verify the packet is a SYN-ACK packet
            else if (inTCP.verifyMessage(1, this.finBytSeqNum +1, 0, 1, 1) == true) {
                System.out.println("Received packet is an FIN-ACK packet. Closing connection.");
                break;
            }

        }

        // Store the received packet in the message list
        this.messageListIn.add(inTCP);

        // Create a new TCP message that is an ACK packet
        TCPmessageStatus outTCP2 = new TCPmessageStatus(this.finBytSeqNum +1, inTCP.byteSequenceNumber + 1);
        outTCP2.setDatalessMessage(0, 0, 1); // SYN = 0, FIN = 0, ACK = 1
        this.messageListOut.add(outTCP2);

        // Send the ack packet 3 times and don't wait for a response
        sendAndWaitForResponse(outTCP2, false); 
        sendAndWaitForResponse(outTCP2, false);
        sendAndWaitForResponse(outTCP2, false);

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
        
        // Convert data to TCP message
        TCPmessageStatus tcpMessage = new TCPmessageStatus(data);
        
        // Create a new DatagramPacket with the specified data and address
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, this.targetIPAddress, this.targetPort);
            
            // Confirm the details of the sent packet
            // if (this.extra_logging) {
            //     System.out.println("Sent packet details ---- create packet 2");
            //     TCPmessageStatus tcpMessageSent = new TCPmessageStatus(packet.getData());
            //     tcpMessageSent.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
            //     System.out.println("Above is analystis of just sent packet.");
            // }

            return packet;
        } catch (Exception e) {
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
            
            DatagramSocket socket = new DatagramSocket(port);

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
     * @param waitForResponse
     * @return packet
     * @throws IOException
     */
    private TCPmessageStatus sendAndWaitForResponse(TCPmessageStatus tcpMessage, boolean waitForResponse) { // Add triplicate packet option for parameter
        
        // Initialize
        DatagramPacket outPacket = null;

        // Create a new DatagramPacket to send the data if not void
        if (tcpMessage != null) {
            // Print details of the message
            // if (this.extra_logging) {
            //     System.out.println("TCP data sendandwait ---- ");
            //     tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
            //     tcpMessage.message.printData();
            // }
            byte[] data = tcpMessage.getMessage();
            outPacket = createPacket(data);
        } 

        // Create a new DatagramPacket to receive the data
        byte[] buffer = new byte[this.maxBytes];
        DatagramPacket responsePacket = null;


        // This is where we do triplicate packet if needed
        // Send a packet using the socket if there is one
        if (outPacket != null) {
            try {
            sendPacket(outPacket, tcpMessage);
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e.getMessage());
            }
        }

        // Check if there needs to be a response
        if (waitForResponse == false) {
            return null; // No need to wait for a response
        }
        
        // Wait for the client to send a response packet
        try {
            responsePacket = receivePacket(); 
        } catch (IOException e) {
            System.out.println("Error receiving packet: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.out.println("Socket timed out: " + e.getMessage());
            return null;
        }

        // Check if the packet is null
        if (responsePacket == null) {
            System.out.println("No packet received within the timeout period.");
            return null;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCV = new TCPmessageStatus(responsePacket.getData());


        // Print the received message details
        if (tcpMessageRCV == null) {
            System.out.println("Error parsing TCP message.");
            return null;
        }
        // tcpMessageRCV.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());

        // Return the received packet
        return tcpMessageRCV;
    }

    /**
     * This method will send a packet using the socket.
     * @param packet
     * @param tcpMessage
     * @throws IOException
     */
    private void sendPacket(DatagramPacket packet, TCPmessageStatus tcpMessage) throws IOException {
        // Send the packet using the socket
        this.socket.send(packet);

        // Log the sent packet
        tcpMessage.sent = true;
        System.out.println("Sent packet details ---- Send Packet");
        tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
    }

    /**
     * This method will listen for incoming packets on the socket.
     * @param packet
     * @param waitTime in nanoseconds
     * @return
     * @throws IOException
     */
    private DatagramPacket receivePacket() throws IOException {
        
        // Create datagram packet to receive data
        byte[] buffer = new byte[this.maxBytes];
        DatagramPacket packet = new DatagramPacket(buffer, this.maxBytes);

        // Receive the packet using the socket
        this.socket.receive(packet);
        
        // Convert to TCPmessageStatus to print received message details
        TCPmessageStatus tcpMessage = new TCPmessageStatus(packet.getData());
        // Print the received message details
        System.out.println("Received packet details ---- Receive Packet");
        tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
        tcpMessage.received = true;
        
        // Return the packet
        return packet;
    }

    
}