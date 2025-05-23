/**
 * This class will track the data received successfully over TCP.
 * There will be an attribute list tracking start point and end point of each message that will be ordered.
 * -- Each item in the list will be a pair of integers.
 * -- The first integer will be the start point of the message.
 * -- The second integer will be the end point of the message.
 * -- The list will be ordered by the start point of each message.
 * There will be an attribute to track the highest acknowledged number that can be sent.
 * There will be method that takes a message and indicates if it can be acknowledged.
 * Will keep track of received data, anything that has been stored off fully will be discarded.
 */

import java.util.ArrayList;

public class TCPdataTracker {
    // Attributes for sender or reciever
    // List of bytes
    private ArrayList<byte[]> dataManaged;
    private ArrayList<Integer> startPoints;
    private ArrayList<Integer> completedData;
    private TCPfileHandling fileHandler;
    private int expectedChunkSize;

    // Attributes for the sender
    private int lastByteAcked;
    private int lastByteWritten;
    private int lastByteSent;
    private int maxSendBuffer;
    private int currentChunkSize;

    // Attributes for the receiver
    private int lastByteRead;
    private int lastByteRcvd;
    private int maxRcvBuffer;


    // For sender and receiver
    public TCPdataTracker(boolean isSender, int chunkSize, int maxBuffer, TCPfileHandling fileHandler) {
        // Initialize the data tracker
        this.fileHandler = fileHandler;
        this.expectedChunkSize = chunkSize;
        this.completedData = new ArrayList<>();

        if (isSender) {
            // Initialize sender attributes
            this.lastByteAcked = 0;
            this.lastByteWritten = 0;
            this.lastByteSent = 0;
            this.maxSendBuffer = maxBuffer;
            this.currentChunkSize = 0;
        } else {
            // Initialize receiver attributes
            this.lastByteRead = 0;
            this.lastByteRcvd = 1;
            this.maxRcvBuffer = maxBuffer;
            this.startPoints = new ArrayList<>();
        }        
    }

    // For receiver, returns true if the packet can be acknowledged
    public boolean receiverAddData(int start, int length, byte[] data) {
        
        // Store start point
        this.startPoints.add(start);   

        // Add the data using the file handler
        try {
            fileHandler.writeByteArrayToFile(data, length);
        } catch (Exception e) {
            System.out.println("Error writing data to file: " + e.getMessage());
            return false;
        }
        // Update the last byte received
        this.lastByteRcvd += length;
        this.lastByteRead += length;
        
        return true;
    }

    // Check if the data has been received already
    public boolean isDataReceived(int startPoint) {
        // Check if the start point is already in the list
        for (int i = 0; i < this.startPoints.size(); i++) {
            if (this.startPoints.get(i) == startPoint) {
                return true;
            }
        }
        return false;
    }

    // Next expected byte to be received
    public int getNextExpectedByte() {
        return this.lastByteRcvd;
    }

    // For sender....
    public byte[] senderRetrieveData() {
        byte[] data = null;
        // Use hasnextchunk to pull next chunk of data
        try {
            data = fileHandler.readNextChunk();
        } catch (Exception e) {
            System.out.println("Error retrieving data chunk: " + e.getMessage());
            return null;
        }
        if (data == null) {
            this.currentChunkSize = 0;
            return null;
        }

        // Update the last byte sent
        this.lastByteSent += data.length;
        this.currentChunkSize = data.length;
        
        return data;
    }

    // Acknowledge the data
    public void senderAckData() {

        // Update the last byte acknowledged
        this.lastByteAcked += this.currentChunkSize;
    }

    // Check if there is data to send
    public boolean hasDataToSend() {
        // Check if there is data to send
        return this.lastByteSent < this.lastByteAcked + this.maxSendBuffer;
    }

    // Copmpleted data
    public void addAckedData(int ackNumber) {
        // Add the completed data to the list
        this.completedData.add(ackNumber);
    }

    // Is data previously acknowledged
    public boolean isDataAcked(int ackNumber) {
        if (this.completedData.isEmpty()) {
            return false; // No data has been acknowledged yet
        }
        // Check if the ack number is in the list
        for (int testAck : this.completedData) {
            if (testAck == ackNumber) {
                return true;
            }
        }
        return false;
    }
}