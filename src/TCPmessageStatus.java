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
     * Returns a byte array for a message without data
     * SYN, FIN, ACK should be 0 or 1
     * @return byte array with the sender startup message
     */
    public byte[] getDatalessMessage(int SYN, int FIN, int ACK) {
        
        // Verify the parameters
        if (SYN < 0 || SYN > 1) {
            throw new IllegalArgumentException("SYN must be 0 or 1");
        }
        if (FIN < 0 || FIN > 1) {
            throw new IllegalArgumentException("FIN must be 0 or 1");
        }
        if (ACK < 0 || ACK > 1) {
            throw new IllegalArgumentException("ACK must be 0 or 1");
        }

        // Set parameters
        this.SYN = SYN;
        this.FIN = FIN;
        this.ACK = ACK;

        // Get time stamp in nanoseconds
        long timeStamp = System.nanoTime();

        // Generate an empty byte array
        byte[] blankData = new byte[0];

        // Generate a TCP message with the data
        TCPheader message = new TCPheader(byteSequenceNumber,acknowledgmentNumber,timestamp,0,SYN,FIN,ACK,blankData);

        // Return the byte array
        return message.returnFullHeader();
    }

    /**
     * Returns a byte array for a message with data
     * SYN, FIN, ACK should be 0 or 1
     * @param data byte array with the data
     * @return byte array with the sender startup message
     */
    public byte[] getDataMessage(int SYN, int FIN, int ACK, byte[] data) {
        
        // Verify the parameters
        if (SYN < 0 || SYN > 1) {
            throw new IllegalArgumentException("SYN must be 0 or 1");
        }
        if (FIN < 0 || FIN > 1) {
            throw new IllegalArgumentException("FIN must be 0 or 1");
        }
        if (ACK < 0 || ACK > 1) {
            throw new IllegalArgumentException("ACK must be 0 or 1");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }

        // Set parameters
        this.SYN = SYN;
        this.FIN = FIN;
        this.ACK = ACK;

        // Get time stamp in nanoseconds
        long timeStamp = System.nanoTime();

        // Generate a TCP message with the data
        TCPheader message = new TCPheader(byteSequenceNumber,acknowledgmentNumber,timestamp,0,SYN,FIN,ACK,data);

        // Return the byte array
        return message.returnFullHeader();
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
        TCPheader message = new TCPheader();
        
        // Parse the header
        if (message.parseReceivedTCP(data) == false) {
            throw new IllegalArgumentException("Message corrupted or invalid.");
        }

        // Set the parameters
        this.byteSequenceNumber = message.byteSequenceNumber;
        this.acknowledgmentNumber = message.acknowledgmentNumber;
        this.SYN = message.SYN;
        this.FIN = message.FIN;
        this.ACK = message.ACK;
        this.timestamp = message.timestamp;
        this.received = true;

        // Determine if there is data
        if (message.dataLength > 0) {
            this.containsData = true;
            this.dataLength = message.dataLength;
        } else {
            this.containsData = false;
            return null;
        }

        // Return the byte array
        return message.data;
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