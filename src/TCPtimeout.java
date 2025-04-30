


public class TCPtimeout {
    //nanoseconds
    private long startTime; 
    public long timeOutTimer; 
    private long ERTT;
    private long EDEV;

    public TCPtimeout() {
        // Set default values
        System.out.println("Timeout of 5 seconds.");
        // 5 seconds in nanoseconds
        this.timeOutTimer = 5 * 1000000000L;
        this.startTime = System.nanoTime();
    }

    public void updateTimeOut(long receivedTime, long sentTime) {
        long SRTT = Math.abs(receivedTime - sentTime);
        long SDEV = Math.abs(SRTT - this.ERTT);
        this.ERTT = (long) (0.875 * this.ERTT + 0.125 * SRTT);
        this.EDEV = (long) (0.75 * this.EDEV + 0.25 * SDEV);
        // Set timeout value
        this.timeOutTimer = this.ERTT + 4 * this.EDEV;
    }

    public void updateTimeOutZero(long receivedTime, long sentTime) {
        this.ERTT = Math.abs(receivedTime - sentTime);
        this.EDEV = 0;
        // Set timeout value
        this.timeOutTimer = 2 * this.ERTT;
    }

    public boolean isTimedOut(long currentTime, long sentTime) {
        long rtt = Math.abs(currentTime - sentTime);
        // Check if the timeout has occurred
        if (rtt > this.timeOutTimer) {
            System.out.println("Timeout occurred.");
            return true; // Timeout occurred
        } else {
            System.out.println("No timeout.");
            return false; // No timeout
        }
        
    }

    public long getStartTime() {
        // Get start time
        return this.startTime;
    }


}