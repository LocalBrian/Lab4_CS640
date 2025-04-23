


public class TCPoverallManager {

    private int TCPstatus;
    private long timeOutTimer;


    public TCPoverallManager(String filePath, int chunkSize) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.chunkSize = chunkSize;
        this.currentPosition = 0;
    }

    public TCPoverallManager(String filePath) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.currentPosition = 0;
    }
        return this.currentChunkSize;
}