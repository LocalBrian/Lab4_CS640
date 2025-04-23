


public class TCPmessageStatus {

    // Status type list and int value
    public static final int STATUS_TYPE = 0;
    public static final int STATUS_TYPE_ACK = 1;
    public static final int STATUS_TYPE_NACK = 2;
    public static final int STATUS_TYPE_DATA = 3;
    public static final int STATUS_TYPE_DATA_ACK = 4;
    public static final int STATUS_TYPE_DATA_NACK = 5;


    // Other parameters
    private boolean sent;
    private boolean acknowledged;
    private int overallType; // 0: N/A, 1: Sent, 2: Received
    private int statusType; // 0: N/A, 1: ACK, 2: NACK, 3: DATA, 4: DATA_ACK, 5: DATA_NACK
    private long sentTime;


    public TCPmessageStatus(String filePath, int chunkSize) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.chunkSize = chunkSize;
        this.currentPosition = 0;
    }

    public TCPmessageStatus(String filePath) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.currentPosition = 0;
    }
        return this.currentChunkSize;
}