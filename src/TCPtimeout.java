


public class TCPtimeout {

    public boolean isActive;
    public boolean isConnected;
    public boolean isClosed;
    private int TCPmode; // 1 for client, 2 for server
    private long timeOutTimer;


    // public TCPtimeout(int TCPmode) {
    //     this.TCPmode = TCPmode;
    //     this.isActive = false;
    //     this.isConnected = false;
    //     this.isClosed = false;
    //     this.filePath = filePath;
    //     this.file = new File(filePath);
    //     this.chunkSize = chunkSize;
    //     this.currentPosition = 0;
    // }

    // public TCPtimeout(String filePath) {
    //     this.filePath = filePath;
    //     this.file = new File(filePath);
    //     this.currentPosition = 0;
    // }
    

    public int TCPtimeout() {
        // Start the TCP connection
        // This is a placeholder for the actual implementation
        // In a real scenario, you would establish a TCP connection here
        System.out.println("Starting TCP connection...");
        return 0; // Return 0 to indicate success
    }
}