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
    private int maxRetries; // Maximum number of retries for sending a packet

    // Project specific parametrs
    private int packetsSent; // Number of packets sent
    private int packetsReceived; // Number of packets received
    private int outOfSequencePacketsDiscarded; // Number of out of sequence packets discarded
    private int badChecksumPacketsDiscarded; // Number of packets discarded due to bad checksum
    private int retransmissions; // Number of retransmissions
    private int duplicateAcksGlobal; // Number of duplicate ACKs received

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
        this.maxUnits = maxUnits;
        this.maxRetries = 16;
        try {
            this.targetIPAddress = InetAddress.getByName(string_ipAddress);
        } catch (UnknownHostException e) {
            System.out.println("Error: " + e.getMessage());
            this.targetIPAddress = null;
        }
        this.messageListIn = new ArrayList <TCPmessageStatus>();
        this.messageListOut = new ArrayList <TCPmessageStatus>();
        this.timeout = new TCPtimeout();
        // Project specific parametrs
        this.packetsSent = 0; // Number of packets sent
        this.packetsReceived = 0; // Number of packets received
        this.badChecksumPacketsDiscarded = 0; // Number of packets discarded due to bad checksum
        this.retransmissions = 0; // Number of retransmissions
        this.duplicateAcksGlobal = 0; // Number of duplicate ACKs received
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
        this.maxRetries = 16;
        // Project specific parametrs
        this.packetsSent = 0; // Number of packets sent
        this.packetsReceived = 0; // Number of packets received
        this.outOfSequencePacketsDiscarded = 0; // Number of out of sequence packets discarded
        this.badChecksumPacketsDiscarded = 0; // Number of packets discarded due to bad checksum
        this.retransmissions = 0; // Number of retransmissions
        this.duplicateAcksGlobal = 0; // Number of duplicate ACKs sent
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
            // Set the socket timer to 150 milliseconds
            try {
                this.socket.setSoTimeout(150);
            } catch (SocketException e) {
                System.out.println("Error setting socket timeout: " + e.getMessage());
                return false;
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
            // Set the socket timer to 100 milliseconds
            try {
                this.socket.setSoTimeout(100);
            } catch (SocketException e) {
                System.out.println("Error setting socket timeout: " + e.getMessage());
                return false;
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
        this.isConnected = true;
        this.isClosed = false;

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
        if (this.TCPmode == TCP_sender) {
            this.printStatisticsClient();
        }
        else if (this.TCPmode == TCP_receiver) {
            this.printStatisticsServer();
        }
        System.out.println("TCP connection closed.");
        this.isOpen = false;
        this.isConnected = false;
        this.isClosed = true;

        // Print the statistics
    }

    public void printStatisticsServer() {
        System.out.println("---------------------------------");
        System.out.println("TCP Server connection statistics:");
        System.out.println("Data Received: " + this.fileHandler.totalData + " bytes");
        System.out.println("Packets sent: " + this.packetsSent);
        System.out.println("Packets received: " + this.packetsReceived);
        System.out.println("Out of sequence packets discarded: " + this.outOfSequencePacketsDiscarded);
        System.out.println("Packets discarded due to incorrect checksum: " + this.badChecksumPacketsDiscarded);
        System.out.println("Retransmissions: " + this.retransmissions);
        System.out.println("Duplicate ACKs sent: " + this.duplicateAcksGlobal);
    }

    public void printStatisticsClient() {
        System.out.println("---------------------------------");
        System.out.println("TCP Client connection statistics:");
        System.out.println("Data Sent: " + this.fileHandler.totalData + " bytes");
        System.out.println("Packets sent: " + this.packetsSent);
        System.out.println("Packets received: " + this.packetsReceived);
        System.out.println("Out of sequence packets discarded: " + this.outOfSequencePacketsDiscarded);
        System.out.println("Packets discarded due to incorrect checksum: " + this.badChecksumPacketsDiscarded);
        System.out.println("Retransmissions: " + this.retransmissions);
        System.out.println("Duplicate ACKs received: " + this.duplicateAcksGlobal);
    }

    /**************************************************** Server communication methods ****************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean serverOpenListeningState() {
        // DatagramPacket workingPacket = new DatagramPacket(buffer, buffer.length);
        DatagramPacket responsePacket = null;
        TCPmessageStatus tcpMessageRCVack = null;
        int attempts = 0;
        long lastSentTime;

        lastSentTime = System.nanoTime();
        // Listen for incoming packets for 60 seconds
        while (attempts < this.maxRetries) {
            
            // Wait for the client to send a SYN packet
            try {
                responsePacket = receivePacket(); 
            } catch (Exception e) {
            }

            // Check if the packet is null
            if (responsePacket == null) {
                if (this.timeout.isTimedOut(System.nanoTime(), lastSentTime)) {
                    lastSentTime = System.nanoTime();
                    attempts++;
                }
                continue; // Retry listening
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

        // Verify the packet is a new SYN packet
        if (tcpMessageRCVinit.verifyMessage(0, 0, 1, 0, 0) == false) {
            return false;
        }

        // Process the received packet
        this.targetIPAddress = responsePacket.getAddress();
        this.targetPort = responsePacket.getPort();

        // Create a new TCP message that is a SYN-ACK packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, tcpMessageRCVinit.byteSequenceNumber + 1);
        outTCP.setDatalessMessage(1, 0, 1, tcpMessageRCVinit.timestamp); // SYN = 1, ACK = 1, FIN = 0

        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        attempts = 0;
        lastSentTime = System.nanoTime();
        sendAndWaitForResponse(outTCP, false);
        long lastReceivedTime = System.nanoTime();
        long maxWaitTime = 30 * 1000000000L; // 30 second in nanoseconds
        while (true) {
            // Check if the packet is null
            if (maxWaitTime < (System.nanoTime() - lastReceivedTime)) {
                System.out.println("No packet received within the timeout period for startup ACK, closing the port and exiting.");
                return false;
            }
            
            // Check if there is a timeout to resend
            if (this.timeout.isTimedOut(System.nanoTime(), lastSentTime)) {
                sendAndWaitForResponse(outTCP, false);
                lastSentTime = System.nanoTime();
                this.retransmissions++;
                attempts++;
            }

            // Listen for a response
            tcpMessageRCVack = sendAndWaitForResponse(null, true);

            // Check if the packet is null
            if (tcpMessageRCVack == null) {
                // Do nothing, just retry
                continue;
            } // Verify the packet is our desired ACK packet
            else if (tcpMessageRCVack.verifyMessage(1, 1, 0, 0, 1)) {
                break;
            }
            lastReceivedTime = System.nanoTime();

        }

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
        int position;
        this.finBytSeqNum = 1;
        long lastReceivedTime = System.nanoTime();
        long maxWaitTime = 30 * 1000000000L; // 30 second in nanoseconds

        // Loop until either a null or a FIN packet is received
        while (connectionLost == false) {

            this.messageListIn = new ArrayList <TCPmessageStatus>();
                
                // Loop gathering packets sent
                while (true) {
                    // Wait for a packet to come in
                    inTCP = sendAndWaitForResponse(null, true);
                    // Exit inner loop if the packet is null
                    if (inTCP == null) {
                        break;
                    } 
                    // Drop the packet if it is for establishing a connection, update last received time
                    lastReceivedTime = System.nanoTime();
                    if (inTCP.verifyMessage(1, 1, 0, 0, 1) == true && inTCP.dataLength == 0) {
                        break; // Skip to the next packet
                    }
                    
                    // Check if the packet has previously been received
                    else if (this.dataTracker.isDataReceived(inTCP.byteSequenceNumber)) {
                        // Send an ACK packet back to the client
                        outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                        outTCP.setDatalessMessage(0, 0, 1, inTCP.timestamp); // SYN = 0, ACK = 1, FIN = 0
                        this.sendAndWaitForResponse(outTCP, false);
                        this.duplicateAcksGlobal++;
                        continue; // Skip to the next packet
                    }
                    // If the list is empty, add the packet
                    else if (this.messageListIn.size() == 0) {
                        this.messageListIn.add(inTCP);
                    }
                    // Check if there is space in message list in
                    else if (this.messageListIn.size() < this.maxUnits) {
                        position = 0;
                        while (position < this.messageListIn.size()) {
                            // Check if the packet is already in the list
                            if (inTCP.byteSequenceNumber == this.messageListIn.get(position).byteSequenceNumber) {
                                break; // Skip to the next message
                            } else if (inTCP.byteSequenceNumber < this.messageListIn.get(position).byteSequenceNumber) {
                                this.messageListIn.add(position, inTCP);
                                break; // Exit the loop if the packet is inserted
                            } else {
                                position++;
                            }
                        }
                        // If the packet is not inserted, add it to the end of the list
                        if (position == this.messageListIn.size()) {
                            this.messageListIn.add(inTCP);
                        }
                    } 
                    // If the message list is full, might need to drop packets
                    else {
                        position = 0;
                        while (position < this.messageListIn.size()) {
                            // Check if the packet is already in the list
                            if (inTCP.byteSequenceNumber == this.messageListIn.get(position).byteSequenceNumber) {
                                break; // Skip to the next message
                            } else if (inTCP.byteSequenceNumber < this.messageListIn.get(position).byteSequenceNumber) {
                                this.messageListIn.remove(this.messageListIn.size() - 1); // Remove the last item
                                this.messageListIn.add(position, inTCP);
                                this.outOfSequencePacketsDiscarded++;
                                break; // Exit the loop if the packet is inserted
                            } else {
                                position++;
                            }
                        }
                        // Drop the packet
                    }
                }
                
                // Check if the list in is empty
                if (this.messageListIn.isEmpty() == true) {
                } 
                // Check if there is a single FIN packet
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
                        // Process the received data bytes
                        this.dataTracker.receiverAddData(inTCP.byteSequenceNumber, inTCP.dataLength, inTCP.getMessage());
                        // Send an ACK packet back to the client
                        outTCP = new TCPmessageStatus(1, inTCP.byteSequenceNumber + inTCP.dataLength);
                        outTCP.setDatalessMessage(0, 0, 1, inTCP.timestamp); // SYN = 0, ACK = 1, FIN = 0
                        this.sendAndWaitForResponse(outTCP, false);
                        // Remove the processed packet from the list
                        this.messageListIn.remove(0);
                    } else {
                        // The first packet isn't what we want - listen for more packets
                        break;
                    }
                                
                }      
        
            if (maxWaitTime < (System.nanoTime() - lastReceivedTime)) {
                System.out.println("No packet received within the timeout period for data, closing the port and exiting.");
                connectionLost = true;
            }

        }

        return false; // Return false to indicate connection was lost
    }

    // Close out communication for the server
    public boolean serverCloseTCPconnection() {
        // Create a new DatagramPacket to receive the data
        TCPmessageStatus inTCP = null;
        int attempts = 0;
        long lastSentTime;
        long lastReceivedTime = System.nanoTime();
        long maxWaitTime = 30 * 1000000000L; // 30 second in nanoseconds

        // Update the timeout timer
        this.timeout.setTimeOut(100); // 100 milliseconds

        //  Create a new TCP message that is a FIN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(1, this.dataTracker.getNextExpectedByte() +1);
        outTCP.setDatalessMessage(0, 1, 1, this.messageListIn.get(0).timestamp); // SYN = 0, ACK = 0, FIN = 1


        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        attempts = 0;
        lastSentTime = System.nanoTime();
        sendAndWaitForResponse(outTCP, false);

        while (maxWaitTime > (System.nanoTime() - lastReceivedTime)) {

            if (this.timeout.isTimedOut(System.nanoTime(), lastSentTime)) {
                sendAndWaitForResponse(outTCP, false);
                lastSentTime = System.nanoTime();
                attempts++;
                this.retransmissions++;
            }
            inTCP = sendAndWaitForResponse(null, true);
            // Check if the packet is null
            if (inTCP == null) {
                continue;
            } // Verify the packet is an SYN-ACK packet
            else if (inTCP.verifyMessage(this.dataTracker.getNextExpectedByte() +1, 2, 0, 0, 1) == true) {
                System.out.println("Received packet is an FIN-ACK packet. Closing connection.");
                break;
            } 
        }

        return true; // Return true to indicate success
    }


    /********************************************************* Client communication methods  ***************************************************/

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean clientEstablishTCPconnection() {
        int attempts;
        TCPmessageStatus inTCP = null;

        //  Create a new TCP message that is a SYN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(0, 0);
        outTCP.setDatalessMessage(1, 0, 0, System.nanoTime()); // SYN = 1, ACK = 0, FIN = 0

        // Keep attempting to send the SYN-ACK packet until it is acknowledged
        attempts = 0;
        sendAndWaitForResponse(outTCP, false);

        while (attempts < this.maxRetries) {
            
            if (this.timeout.isTimedOut(System.nanoTime(), outTCP.timestamp)) {
                outTCP.resetMessage();
                sendAndWaitForResponse(outTCP, false);
                this.retransmissions++;
                attempts++;
            }

            if (attempts == this.maxRetries) {
                System.out.println("No packet received within the timeout period for startup SYN, closing the port and exiting.");
                return false;
            }

            inTCP = sendAndWaitForResponse(null, true);

            // Check if the packet is null
            if (inTCP == null) {
                // Do nothing, just retry
                continue;
            } // Verify the packet is our desired SYN-ACK packet
            else if (inTCP.verifyMessage(0, 1, 1, 0, 1)) {
                break;
            }
        }

        // Update the timeout timer
        this.timeout.updateTimeOutZero(System.nanoTime(), inTCP.timestamp);

        // Create a new TCP message that is an ACK packet
        TCPmessageStatus outTCP2 = new TCPmessageStatus(1, inTCP.byteSequenceNumber + 1);
        outTCP2.setDatalessMessage(0, 0, 1, System.nanoTime()); // SYN = 0, FIN = 0, ACK = 1

        // Send the ack packet in triplicate and don't wait for a response
        sendAndWaitForResponse(outTCP2, false); 
        sendAndWaitForResponse(outTCP2, false);
        sendAndWaitForResponse(outTCP2, false);
        this.retransmissions += 2;

        return true; // Return true to indicate success
    }

    /**
     * This method is for when the client is sending data to the server.
     */
    public boolean clientSendData() {
        // Initialize the variables for controlling the while loop
        int attempts;
        int count;
        TCPmessageStatus tcpMessageData = new TCPmessageStatus(0, 0);
        TCPmessageStatus tcpMessageRCVack = new TCPmessageStatus(0, 0);
        TCPmessageStatus activeMessage = null;
        byte[] data = null;
        int currentWindow = 1;
        int currentByteSqnNumber = 1;
        boolean finalRound = false;
        int activeMessagesAcked;
        boolean resendOccurred = false;
        int duplicateAckCount = 0;
        

        // Loop until either a null or a FIN packet is received
        while (finalRound == false) {

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
                // Set the timestamp for the message
                tcpMessageData.resetMessage();
                // send the packet and store it
                sendAndWaitForResponse(tcpMessageData, false);
                this.messageListOut.add(tcpMessageData);
                count++;
                currentByteSqnNumber += data.length;
            }

            duplicateAckCount = 0;

            while (true) {
            

                // While loop waiting for ACK
                while (true) {
                    // Check if messages should be resent
                    count = 0;
                    while (count < this.messageListOut.size()) {
                        activeMessage = this.messageListOut.get(count);                        
                        if (this.timeout.isTimedOut(System.nanoTime(), activeMessage.timestamp)) {
                            resendOccurred = true;
                            // Check if the message has been sent more than max attempts times
                            if (activeMessage.sendAttempts >= this.maxRetries) {
                                System.out.println("Message has been resent too many times, connection lost.");
                                this.messageListOut.remove(count);
                                return false; // Return false to indicate connection was lost
                            }
                            activeMessage.sendAttempts++;
                            activeMessage.resetMessage();
                            // Replace the message in the list
                            this.messageListOut.set(count, activeMessage);
                            // Resend the message
                            sendAndWaitForResponse(activeMessage, false);
                            this.retransmissions++;
                        }
                        count++;
                    }


                    
                    // Wait for a packet to come in
                    tcpMessageRCVack = sendAndWaitForResponse(null, true);

                    // If the packet is null, break out of the inner loop
                    if (tcpMessageRCVack == null) {
                        break;
                    } 

                    // Update the timeout timer based on the message
                    this.timeout.updateTimeOut(System.nanoTime(), tcpMessageRCVack.timestamp);
                    
                    

                    // See if previously acked
                    if (this.dataTracker.isDataAcked(tcpMessageRCVack.acknowledgmentNumber)) {
                        duplicateAckCount++;
                        this.duplicateAcksGlobal++;
                        continue; // Skip to the next packet
                    }
                    else {
                        // See if it matches one of the messages we sent
                        count = 0;
                        while (count < this.messageListOut.size()) {
                            activeMessage = this.messageListOut.get(count);

                            if (tcpMessageRCVack.verifyMessage( 1, activeMessage.byteSequenceNumber + activeMessage.dataLength, 0, 0, 1) == true) {
                                this.messageListOut.remove(count);
                                this.dataTracker.addAckedData(tcpMessageRCVack.acknowledgmentNumber);
                                break;
                            } 
                            count++;
                        }
                    }



                    if (0 == this.messageListOut.size()) {
                        duplicateAckCount = 0;
                        break; // Exit the loop if all messages are acknowledged
                    }
                    // Resend all unacknowledged messages if duplicate ACKs are received
                    else {
                        if (duplicateAckCount >= 3) {
                            resendOccurred = true;
                            // Resend all active messages
                            count = 0;
                            while (count < this.messageListOut.size()) {
                                activeMessage = this.messageListOut.get(count);
                                activeMessage.sendAttempts++;
                                activeMessage.resetMessage();
                                // Resend the message
                                sendAndWaitForResponse(activeMessage, false);
                                this.retransmissions++;
                                // Replace the message in the list
                                this.messageListOut.set(count, activeMessage);
                                count++;
                            }
                            duplicateAckCount = 0;
                        }
                    }
                }

                // Check if active messages acknowledged is equal to the number of messages sent
                if (0 == this.messageListOut.size()) {
                    // If the first attempt, then increase the window size
                    if (resendOccurred == false) {
                        currentWindow = Math.min(currentWindow *2, this.maxUnits);
                    } else {
                        currentWindow = Math.min(currentWindow /2, this.maxUnits);
                        currentWindow = Math.max(currentWindow, 1);
                    }
                    resendOccurred = false;
                    // All messages have been acknowledged, break out of the loop
                    break;
                }

            }

            }

        if (finalRound == true) {
            return true; // Return true to indicate success
        }

        return false; // Return false to indicate connection was lost
    }

    /**
     * This method is for when the server is attempting to establish a connection with the client.
     */
    public boolean clientCloseTCPconnection() {
        // Create a new DatagramPacket to receive the data
        TCPmessageStatus inTCP = null;
        int attempts = 0;

        //  Create a new TCP message that is a FIN packet
        TCPmessageStatus outTCP = new TCPmessageStatus(this.finBytSeqNum, 1);
        outTCP.setDatalessMessage(0, 1, 0, System.nanoTime()); // SYN = 0, ACK = 0, FIN = 1

        while (attempts < this.maxRetries) {
            
            if (this.timeout.isTimedOut(System.nanoTime(), outTCP.timestamp)) {
                outTCP.resetMessage();
                sendAndWaitForResponse(outTCP, false);
                this.retransmissions++;
                attempts++;
            }

            // Listen for a response
            inTCP = sendAndWaitForResponse(null, true); 

            // Check if the packet is null
            if (inTCP == null) {
                continue;
            } // Verify the packet is a FIN-ACK packet
            else if (inTCP.verifyMessage(1, (outTCP.byteSequenceNumber +1), 0, 1, 1)) {
                System.out.println("Received packet is an FIN-ACK packet. Closing connection.");
                break;
            }

        }

        // Create a new TCP message that is an ACK packet
        TCPmessageStatus outTCP2 = new TCPmessageStatus(this.finBytSeqNum +1, inTCP.byteSequenceNumber + 1);
        outTCP2.setDatalessMessage(0, 0, 1, System.nanoTime()); // SYN = 0, FIN = 0, ACK = 1

        // Send the ack packet 3 times and don't wait for a response
        sendAndWaitForResponse(outTCP2, false); 
        sendAndWaitForResponse(outTCP2, false);
        sendAndWaitForResponse(outTCP2, false);
        this.retransmissions+= 2;

        return true; // Return true to indicate success
    }


    /****************** This code will handle construction of the datagram into properly receivable data including the header. ********************************/

    /**
     * This method will take a byte array and convert it to a string.
     * Don't need inverse, file chunk reader generates byte array.
     * @param data
     * @return
     */
    // private String byteArrayToString(byte[] data) {
    //     StringBuilder sb = new StringBuilder();
    //     for (byte b : data) {
    //         sb.append((char) b);
    //     }
    //     return sb.toString();
    // }

    /**
     * This method will create a packet for sending data.
     * @param data
     * @param offset
     * @param length
     */
    private DatagramPacket createPacket(byte[] data) {
        
        
        
        // Create a new DatagramPacket with the specified data and address
        try {
            DatagramPacket packet = new DatagramPacket(data, 0, data.length, this.targetIPAddress, this.targetPort);

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
            return null;
        } catch (Exception e) {
            return null;
        }

        // Check if the packet is null
        if (responsePacket == null) {
            return null;
        }

        // Parse the TCP data
        TCPmessageStatus tcpMessageRCV = new TCPmessageStatus(responsePacket.getData());


        // Print the received message details
        if (tcpMessageRCV == null) {
            return null;
        }
        

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
        this.packetsSent++;
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

        // Increment counter
        this.packetsReceived++;
        
        // Convert to TCPmessageStatus to print received message details
        TCPmessageStatus tcpMessage = new TCPmessageStatus(packet.getData());
        // Print the received message details
        tcpMessage.printMessageDetails(System.nanoTime() - this.timeout.getStartTime());
        tcpMessage.received = true;

        // Check if the checksum is valid
        if (tcpMessage.message.validateChecksum() == false) {
            this.badChecksumPacketsDiscarded++;
            return null; // Checksum error, drop the packet
        }
        
        // Return the packet
        return packet;
    }

    
}