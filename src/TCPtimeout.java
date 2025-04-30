


public class TCPtimeout {

    private long startTime; 
    public int timeOutTimer; //milliseconds

    public TCPtimeout() {
        // Set default values
        System.out.println("Timeout of 5 seconds.");
        // 5 seconds in milliseconds
        this.timeOutTimer = 5 * 1000;
        this.startTime = System.nanoTime();
        
    }

    public void setTimeOut(int timeOut) {
        // Set timeout value
        this.timeOutTimer = timeOut;
    }

    public long getStartTime() {
        // Get start time
        return this.startTime;
    }
}