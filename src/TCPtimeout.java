


public class TCPtimeout {

    private long startTime; 
    public long timeOutTimer;    

    public void TCPtimeout() {
        // Set default values
        System.out.println("Timeout of 5 seconds.");
        // 5 seconds in nanoseconds
        this.timeOutTimer = 5 * 1000000000L; // 5 seconds in nanoseconds
        this.startTime = System.nanoTime();
        
    }

    public void setTimeOut(long timeOut) {
        // Set timeout value
        this.timeOutTimer = timeOut;
    }

    public long getTimeOut() {
        // Get timeout value
        return this.timeOutTimer;
    }

    public long getStartTime() {
        // Get start time
        return this.startTime;
    }
}