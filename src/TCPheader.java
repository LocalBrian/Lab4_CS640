


public class TCPheader {
    private int byteSequenceNumber;
    private int acknowledgmentNumber;
    private long timestamp;
    private int dataLength;
    private int SYN;
    private int FIN;
    private int ACK;
    private int checksum;
    private byte[] data;
    private byte[] fullHeader;

    public TCPheader() {
        this.byteSequenceNumber = 0;
        this.acknowledgmentNumber = 0;
        this.timestamp = 0;
        this.dataLength = 0;
        this.SYN = 0;
        this.ACK = 0;
        this.FIN = 0;
        this.checksum = 0;
        this.data = new byte[0];
    }

    public TCPheader(int byteSequenceNumber, int acknowledgmentNumber, long timestamp, int dataLength, int SYN, int FIN, int ACK, byte[] data) {
        
        // Initialize the TCP header fields
        this.byteSequenceNumber = byteSequenceNumber;
        this.acknowledgmentNumber = acknowledgmentNumber;
        this.timestamp = timestamp;
        this.dataLength = dataLength;
        this.SYN = SYN;
        this.FIN = FIN;
        this.ACK = ACK;
        this.data = data;

        // Store the header fields in a byte array
        buildHeaderStart();

        // Calculate the checksum
        calculateChecksum();

    }

    public byte[] returnFullHeader(){
        return this.fullHeader;
    }

    private void buildHeaderStart() {
        // Create a byte array to hold the full header
        this.fullHeader = new byte[24 + this.data.length];
        // Set the byte sequence number
        byte[] byteSequenceNumberArray = convertIntToByte(this.byteSequenceNumber);
        System.arraycopy(byteSequenceNumberArray, 0, this.fullHeader, 0, byteSequenceNumberArray.length);
        // Set the acknowledgment number
        byte[] acknowledgmentNumberArray = convertIntToByte(this.acknowledgmentNumber);
        System.arraycopy(acknowledgmentNumberArray, 0, this.fullHeader, 4, acknowledgmentNumberArray.length);
        // Set the timestamp
        byte[] timestampArray = convertLongToByte(this.timestamp);
        System.arraycopy(timestampArray, 0, this.fullHeader, 8, timestampArray.length);
        // Set the data length and statuses
        setLengthAndStatus();
        // Set the checksum
        byte[] checksumArray = convertIntToByte(0);
        System.arraycopy(checksumArray, 0, this.fullHeader, 20, checksumArray.length);
        // Set the data
        System.arraycopy(this.data, 0, this.fullHeader, 24, this.data.length);
    }

    public void setLengthAndStatus() {
        
        // Bit shift left 3 places to make space for the flags
        int shiftedLength = this.dataLength << 3;
        // Set the SYN, FIN, and ACK flags
        this.SYN = (this.SYN & 0x1) << 2;
        this.FIN = (this.FIN & 0x1) << 1;
        this.ACK = (this.ACK & 0x1);
        // Combine the length and flags into a single integer
        int lengthStatus = shiftedLength | this.SYN | this.FIN | this.ACK;
        // Convert the length and status to a byte array
        byte[] lengthStatusArray = convertIntToByte(lengthStatus);
        // Set the length and status in the full header
        System.arraycopy(lengthStatusArray, 0, this.fullHeader, 16, lengthStatusArray.length);
    }

    public void calculateChecksum() {
        // Reset the checksum bytes to 0
        this.checksum = 0;
    }

    public byte[] convertIntToByte(int value) {
        // Convert an integer to a byte array
        byte[] byteArray = new byte[4];
        for (int i = 0; i < 4; i++) {
            byteArray[i] = (byte) ((value >> (8 * (3 - i))) & 0xFF);
        }
        return byteArray;
    }

    public byte[] convertLongToByte(long value) {
        // Convert a long to a byte array
        byte[] byteArray = new byte[8];
        for (int i = 0; i < 8; i++) {
            byteArray[i] = (byte) ((value >> (8 * (7 - i))) & 0xFF);
        }
        return byteArray;
    }

    public byte[] converShortToByte(short value) {
        // Convert a short to a byte array
        byte[] byteArray = new byte[2];
        for (int i = 0; i < 2; i++) {
            byteArray[i] = (byte) ((value >> (8 * (1 - i))) & 0xFF);
        }
        return byteArray;
    }
}