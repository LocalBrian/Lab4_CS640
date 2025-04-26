


public class TCPheader {
    public int byteSequenceNumber;
    public int acknowledgmentNumber;
    public long timestamp;
    public int dataLength;
    public int SYN;
    public int FIN;
    public int ACK;
    public byte[] checksum;
    public byte[] data;
    public byte[] fullHeader;
    public boolean checksumValid;

    public TCPheader() {
        this.byteSequenceNumber = 0;
        this.acknowledgmentNumber = 0;
        this.timestamp = 0;
        this.dataLength = 0;
        this.SYN = 0;
        this.ACK = 0;
        this.FIN = 0;
        this.checksum = new byte[2];
        this.data = new byte[0];
        this.checksumValid = true;
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

        // Validate the checksum
        if (!validateChecksum()) {
            System.out.println("Checksum is invalid");
            this.printData();
        } 

    }

    // Use this for parsing a received TCP header
    public boolean parseReceivedTCP(byte[] fullHeader) {
        // Set the data passed in
        this.fullHeader = fullHeader;
        
        // Validate the checksum
        if (this.validateChecksum() == false) {
            System.out.println("Checksum mismatch");
            return false;
        };
        
        // Parse the received TCP header
        this.byteSequenceNumber = convertByteToInt(fullHeader, 0);
        this.acknowledgmentNumber = convertByteToInt(fullHeader, 4);
        this.timestamp = convertByteToLong(fullHeader, 8);
        this.dataLength = (convertByteToInt(fullHeader, 16) >> 3) & 0xFFFF;
        this.SYN = (fullHeader[16] >> 2) & 0x1;
        this.FIN = (fullHeader[16] >> 1) & 0x1;
        this.ACK = fullHeader[16] & 0x1;
        
        this.data = new byte[this.dataLength];
        System.arraycopy(fullHeader, 24, this.data, 0, this.dataLength);
        
        // Print the parsed header fields
        this.printData();

        return true;
    }

    public byte[] returnFullHeader(){
        return this.fullHeader;
    }

    private void buildHeaderStart() {
        // Create a byte array to hold the full header
        this.fullHeader = new byte[24 + this.dataLength];
        // Print the header length
        System.out.println("Data length: " + this.data.length);
        System.out.println("Header length: " + this.fullHeader.length);
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
        // Add 2 bytes of padding
        byte[] padding = new byte[2];
        padding = convertShortToByte((short) 0);
        System.arraycopy(padding, 0, this.fullHeader, 20, padding.length);
        // Set the checksum to 0
        byte[] checksumArray = convertShortToByte((short) 0);
        System.arraycopy(checksumArray, 0, this.fullHeader, 22, checksumArray.length);
        // Set the data
        System.arraycopy(this.data, 0, this.fullHeader, 24, this.data.length);
        // Calculate the checksum -- also updates the checksum within the header
        checksumArray = calculateChecksum();
        System.arraycopy(checksumArray, 0, this.fullHeader, 22, checksumArray.length);
        // Print the checksum array bits
        System.out.println("Checksum array before calculation: ");
        printByteBits(checksumArray);

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

    /**  
     * Calculate the checksum of the TCP header
     * The checksum is calculated by summing the 16-bit words of the header
     * and taking the one's complement of the sum.
     * This will return 0 if the checksum is correct.
     * The checksum return the complement checksum if this is a initial calculation.
     */
    public byte[] calculateChecksum() {
        
        // Set the checksum bytes to 0
        short checksumArray1 = 0;
        short checksumArray2 = 0;

        // Needed initializations
        short byte1 = 0;
        short byte2 = 0;

        // loop over 16 bits of the header data, 2 bytes at a time
        // if there are less than 2 bytes left, pad with 0s
        for (int i = 0; i < this.fullHeader.length; i += 2) {
            // Get the second byte first
            if (i + 1 < this.fullHeader.length) {
                // Get the second byte
                byte2 = (short) this.fullHeader[i + 1];
                byte2 &= 0x00FF; // Mask to get the last 8 bits
            } else {
                // Pad with 0s
                byte2 = 0;
            }

            // Add byte2 and checksumArray2
            checksumArray2 += byte2;

            // Check for overflow
            if (checksumArray2 > 0x00FF) {
                checksumArray1 += (checksumArray2 & 0xFF00) >>> 8;
                checksumArray2 &= 0x00FF;
            } 
            
            // Get the first byte
            byte1 = (short) this.fullHeader[i];
            byte1 &= 0x00FF; // Mask to get the last 8 bits

            // Add byte1 and checksumArray1
            checksumArray1 += byte1;

            // Check for overflow
            if (checksumArray1 > 0x00FF) {
                checksumArray2 += (checksumArray1 & 0xFF00) >>> 8;
                checksumArray1 &= 0x00FF;
            } 
        }

        // Check if there is overflow in checksumArray2
        if (checksumArray2 > 0x00FF) {
            checksumArray1 += (checksumArray2 & 0xFF00) >>> 8;
            checksumArray2 &= 0x00FF;
        }
        // Check if there is overflow in checksumArray1
        if (checksumArray1 > 0x00FF) {
            checksumArray2 += (checksumArray1 & 0xFF00) >>> 8;
            checksumArray1 &= 0x00FF;
        }

        // Combine the two checksums, 1 is the high bytes and 2 is the low bytes
        // Convert the checksum to a byte array
        byte[] complementCheckSum = new byte[2];
        complementCheckSum[0] = (byte) (~checksumArray1);
        complementCheckSum[1] = (byte) (~checksumArray2);

        
        this.checksum = complementCheckSum;
        
        // Return the computated value
        return complementCheckSum;
        

    }

    /**
     * Validate the checksum
     */
    public boolean validateChecksum() {
        // Calculate the checksum
        byte[] calculatedChecksum = calculateChecksum();
        // Check if the checksum is valid
        if (calculatedChecksum[0] == 0 && calculatedChecksum[1] == 0) {
            this.checksumValid = true;
            return true;
        } else {
            this.checksumValid = false;
            return false;
        }
    }

    public int convertByteToInt(byte[] byteArray, int startIndex) {
        // Convert a byte array to an integer
        int value = 0;
        for (int i = 0; i < 4; i++) {
            value |= (byteArray[startIndex + i] & 0xFF) << (8 * (3 - i));
        }
        return value;
    }
    public long convertByteToLong(byte[] byteArray, int startIndex) {
        // Convert a byte array to a long
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (byteArray[startIndex + i] & 0xFF)) << (8 * (7 - i));
        }
        return value;
    }
    public short convertByteToShort(byte[] byteArray, int startIndex) {
        // Convert a byte array to a short
        short value = 0;
        for (int i = 0; i < 2; i++) {
            value |= (byteArray[startIndex + i] & 0xFF) << (8 * (1 - i));
        }
        return value;
    }


    public byte[] convertIntToByte(int value) {
        // Convert an integer to a byte array
        byte[] byteArray = new byte[4];
        for (int i = 0; i < 4; i++) {
            byteArray[i] = (byte) ((value >>> (8 * (3 - i))) & 0xFF);
        }
        return byteArray;
    }

    public byte[] convertLongToByte(long value) {
        // Convert a long to a byte array
        byte[] byteArray = new byte[8];
        for (int i = 0; i < 8; i++) {
            byteArray[i] = (byte) ((value >>> (8 * (7 - i))) & 0xFF);
        }
        return byteArray;
    }

    public byte[] convertShortToByte(short value) {
        // Convert a short to a byte array
        byte[] byteArray = new byte[2];
        for (int i = 0; i < 2; i++) {
            byteArray[i] = (byte) ((value >>> (8 * (1 - i))) & 0xFF);
        }
        return byteArray;
    }

    /**
     * Print bytes as text
     * @param bytes
     */
    public void printByteBits(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            // Mask with 0xFF to avoid sign extension and get unsigned value
            String bits = String.format("%8s", Integer.toBinaryString(bytes[i] & 0xFF)).replace(' ', '0');
            System.out.println("byte[" + i + "]: " + bits);
        }
    }

    /**
     * Print data
     * @param bytes
     */
    public void printData() {
        // Print the parsed header fields
        System.out.println("Byte Sequence Number: " + this.byteSequenceNumber);
        System.out.println("Acknowledgment Number: " + this.acknowledgmentNumber);
        System.out.println("Timestamp: " + this.timestamp);
        System.out.println("Data Length: " + this.dataLength);
        System.out.println("SYN: " + this.SYN);
        System.out.println("FIN: " + this.FIN);
        System.out.println("ACK: " + this.ACK);
        System.out.println("Data: " + new String(this.data));
    }

}