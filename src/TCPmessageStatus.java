import java.lang.StringBuilder;


public class TCPmessageStatus {

    // Status type list and int value
    // public static final int STATUS_TYPE = 0;
    // public static final int STATUS_TYPE_ACK = 1;
    // public static final int STATUS_TYPE_NACK = 2;
    // public static final int STATUS_TYPE_DATA = 3;
    // public static final int STATUS_TYPE_DATA_ACK = 4;
    // public static final int STATUS_TYPE_DATA_NACK = 5;


    // Other parameters
    public boolean sent;
    public boolean received;
    public boolean acknowledged;
    public boolean containsData;
    public int SYN;
    public int FIN;
    public int ACK;
    public int byteSequenceNumber;
    public int acknowledgmentNumber;
    public int dataLength;
    public long timestamp;
    public TCPheader message;

    // Possibly unneeded
    // private int overallType; // 0: N/A, 1: Sent, 2: Received
    // private int statusType; // 0: N/A, 1: ACK, 2: NACK, 3: DATA, 4: DATA_ACK, 5: DATA_NACK
    // private long sentTime;

    /**
     * Generates a message to start with
     */
    public TCPmessageStatus(int byteSequenceNumber, int acknowledgmentNumber) {
        
        // Verify the parameters
        if (byteSequenceNumber < 0) {
            throw new IllegalArgumentException("byteSequenceNumber must be greater than or equal to 0");
        }
        if (acknowledgmentNumber < 0) {
            throw new IllegalArgumentException("acknowledgmentNumber must be greater than or equal to 0");
        }

        // Set parameters
        this.byteSequenceNumber = byteSequenceNumber;
        this.acknowledgmentNumber = acknowledgmentNumber;
        
        // Set defaults
        this.sent = false;
        this.acknowledged = false;
        this.SYN = 0;
        this.FIN = 0;
        this.ACK = 0;
    }

    /**
     * Process the message as a new instance
     * @param byteMessage
     */
    public TCPmessageStatus(byte[] byteMessage) {
        
        // Verify the parameters
        if (byteMessage == null) {
            throw new IllegalArgumentException("byteMessage must not be null");
        }

        // Generate a TCP header with the data
        TCPheader message = new TCPheader();

        System.out.println("Parsing message: " + byteMessage.length);
        
        // Parse the header
        if (message.parseReceivedTCP(byteMessage) == false) {
            throw new IllegalArgumentException("Message corrupted or invalid.");
        }

        System.out.println("Parsed message: " + message.dataLength);

        // Set the parameters
        this.byteSequenceNumber = message.byteSequenceNumber;
        this.acknowledgmentNumber = message.acknowledgmentNumber;
        this.SYN = message.SYN;
        this.FIN = message.FIN;
        this.ACK = message.ACK;
        this.timestamp = message.timestamp;
        this.dataLength = message.dataLength;
        this.received = true;
        this.containsData = (message.dataLength > 0);
        this.sent = false;
        this.acknowledged = false;
        this.message = message;
    }

    /**
     * Returns a byte array for a message without data
     * SYN, FIN, ACK should be 0 or 1
     * @return byte array with the sender startup message
     */
    public void setDatalessMessage(int eSYN, int eFIN, int eACK) {
        
        // Verify the parameters
        if (eSYN < 0 || eSYN > 1) {
            throw new IllegalArgumentException("SYN must be 0 or 1");
        }
        if (eFIN < 0 || eFIN > 1) {
            throw new IllegalArgumentException("FIN must be 0 or 1");
        }
        if (eACK < 0 || eACK > 1) {
            throw new IllegalArgumentException("ACK must be 0 or 1");
        }

        // Set parameters
        this.SYN = eSYN;
        this.FIN = eFIN;
        this.ACK = eACK;

        // Get time stamp in nanoseconds
        long timeStamp = System.nanoTime();

        // Generate an empty byte array
        byte[] blankData = new byte[0];

        // Generate a TCP message with the data
        TCPheader message = new TCPheader(byteSequenceNumber,acknowledgmentNumber,timestamp,0,eSYN,eFIN,eACK,blankData);
        this.message = message;
        this.dataLength = 0;
        this.containsData = false;
    }

    /**
     * Returns a byte array for a message with data
     * SYN, FIN, ACK should be 0 or 1
     * @param data byte array with the data
     * @return byte array with the sender startup message
     */
    public void setDataMessage(int byteSqnNum, int ackNum, byte[] data) {

        // Set parameters
        this.SYN = 0;
        this.FIN = 0;
        this.ACK = 1;
        this.byteSequenceNumber = byteSqnNum;
        this.acknowledgmentNumber = ackNum;
        this.dataLength = data.length;
        this.containsData = true;
        this.sent = true;
        this.received = false;
        this.acknowledged = false;

        // Get time stamp in nanoseconds
        long timeStamp = System.nanoTime();

        // Generate a TCP message with the data
        TCPheader message = new TCPheader(byteSqnNum,ackNum,timestamp,0,this.SYN,this.FIN,this.ACK,data);
        this.message = message;
    }

    /**
     * Returns a byte array for a message with data
     * @return byte array with the sender startup message
     */
    public byte[] getMessage() {
        
        // Verify the parameters
        if (this.message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        // Return the byte array
        return this.message.returnFullHeader();
    }

    /**
     * Parses a byte array to get the message
     * @param data byte array with the data
     * @return a byte array of the body of the TCP message
     */
    public byte[] parseMessage(byte[] data) {
        
        // Verify the parameters
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        // Generate a TCP header with the data
        TCPheader message1 = new TCPheader();
        
        // Parse the header
        if (message1.parseReceivedTCP(data) == false) {
            throw new IllegalArgumentException("Message corrupted or invalid.");
        }

        // Set the parameters
        this.byteSequenceNumber = message1.byteSequenceNumber;
        this.acknowledgmentNumber = message1.acknowledgmentNumber;
        this.SYN = message1.SYN;
        this.FIN = message1.FIN;
        this.ACK = message1.ACK;
        this.timestamp = message1.timestamp;
        this.received = true;
        this.message = message1;

        // Determine if there is data
        if (message1.dataLength > 0) {
            this.containsData = true;
            this.dataLength = message1.dataLength;
        } else {
            this.containsData = false;
            return null;
        }

        // Return the byte array
        return message1.data;
    }

    /**
     * Verify the TCP message is as expected
     * @param expectedByteSequenceNumber
     * @param expectedAcknowledgmentNumber
     * @param SYN
     * @param FIN
     * @param ACK
     */
    public boolean verifyMessage(int expectedByteSequenceNumber, int expectedAcknowledgmentNumber, int eSYN, int eFIN, int eACK) {
        
        // Confirm the checksum is correct
        if (this.message.validateChecksum() == false) {
            System.out.println("Checksum error.");
            return false;
        }

        // Verify the parameters
        if (expectedByteSequenceNumber != this.byteSequenceNumber) {
            System.out.println("Expected byte sequence number: " + expectedByteSequenceNumber + " but got: " + this.byteSequenceNumber);
            return false;
        }
        if (expectedAcknowledgmentNumber != this.acknowledgmentNumber) {
            System.out.println("Expected acknowledgment number: " + expectedAcknowledgmentNumber + " but got: " + this.acknowledgmentNumber);
            return false;
        }
        if (eSYN != this.SYN) {
            System.out.println("Expected SYN: " + eSYN + " but got: " + this.SYN);
            return false;
        }
        if (eFIN != this.FIN) {
            System.out.println("Expected FIN: " + eFIN + " but got: " + this.FIN);
            return false;
        }
        if (eACK != this.ACK) {
            System.out.println("Expected ACK: " + eACK + " but got: " + this.ACK);
            return false;
        }

        // If all checks pass, return true
        return true;
    }

    /**
     * Print the message details
     */
    public void printMessageDetails(long timeSinceStartUp) {
        // Should be a single string that will be built up based on parameters
        // Initialize empty string
        StringBuilder messageDetails = new StringBuilder();

        // Add the detail of sent or received
        if (this.sent) {
            messageDetails.append("snd ");
        } else if (this.received) {
            messageDetails.append("rcv ");
        } else {
            messageDetails.append("N/A ");
        }

        // Add the time since startup, should be a float with 3 decimal places
        messageDetails.append(String.format("%4.3f ", timeSinceStartUp / 1000000000.0));

        // Add the flag for SYN
        if (this.SYN == 1) {
            messageDetails.append("S ");
        } else {
            messageDetails.append("- ");
        }
        // Add the flag for ACK
        if (this.ACK == 1) {
            messageDetails.append("A ");
        } else {
            messageDetails.append("- ");
        }
        // Add the flag for FIN
        if (this.FIN == 1) {
            messageDetails.append("F ");
        } else {
            messageDetails.append("- ");
        }
        // Add the flag for Data
        if (this.containsData) {
            messageDetails.append("D ");
        } else {
            messageDetails.append("- ");
        }

        // Add the byte sequence number
        messageDetails.append(String.format("%d ", this.byteSequenceNumber));
        // Add the number of bytes in the data
        messageDetails.append(String.format("%d ", this.dataLength));
        // Add the acknowledgment number
        messageDetails.append(String.format("%d ", this.acknowledgmentNumber));

        // Print the one line of details
        System.out.println(messageDetails.toString());

    }
    
}