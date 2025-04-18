


public class TCPheader {
    private int byteSequenceNumber;
    private int acknowledgmentNumber;
    private int timestampe;
    private int length;
    private int SYN;
    private int ACK;
    private int FIN;
    private int checksum;

    public TCPheader(int sourcePort, int destPort, int seqNum, int ackNum, int dataOffset, int reserved, int flags, int windowSize, int checksum, int urgentPointer) {

        this.checksum = checksum;
    }
}