public class TCPend {

    // Flags for which end of TCP this is
    private byte TCP_sender = 1;
    private byte TCP_receiver = 2;
    private byte tcp_type;

    // Arguments that can be passed in 
    private short listening_port; // Port that will be used for communication
    private short target_port; // Port at remote host that will be targetted
    private int target_ipAddress; // IP address to communicate to
    private String file_name; // Filename that will be used
    private int mtu; // Maximum Transmission Unit in bytes
    private int window_size; // Sliding window size in number of segments

    // Other Attributes
    private boolean extra_logging = true; // Change this flag based on level of logging needed

    public static void main(String[] args) {
        System.out.println("Executing TCPend on host.");

        // Create an instance of TCPend
        TCPend tcpE = new TCPend();

        // Verify there was an appropriate amount of arguments sent
        if (args.length != 12) {
            if (args.length != 8) {
                System.out.println("An incorrect number of arguements was sent for a sender or receiver.");
                return;
            }
        }

        // Parse the arguments
        tcpE.parseArgs(args);

    }

    private void parseArgs(String[] args) {
        
        // Default to receiver, will update if argument for sender is received
        tcp_type = TCP_receiver;

        // Parse arguments
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.equals("-p"))
			{ listening_port = Short.parseShort(args[++i]); }
			else if(arg.equals("-s"))
            { target_ipAddress = TCPtoIPv4Address(args[++i]);
              tcp_type = TCP_sender; }
			else if (arg.equals("-a"))
			{ target_port = Short.parseShort(args[++i]); }
			else if (arg.equals("-f"))
			{ file_name = args[++i]; }
			else if (arg.equals("-m"))
			{ mtu = Integer.parseInt(args[++i]); }
			else if (arg.equals("-c"))
			{ window_size = Integer.parseInt(args[++i]); }
		}

        // Verify that the needed arguments for Listener or sender were given.
        if (tcp_type == TCP_sender) {
            if (listening_port == 0 || target_ipAddress == 0 || target_port == 0 || file_name == null || mtu == 0 || window_size == 0) {
                System.out.println("An argument was missing for the sender.");
                return;
            }
        } else {
            if (listening_port == 0 || file_name == null || mtu == 0 || window_size == 0) {
                System.out.println("An argument was missing for the receiver.");
                return;
            }
        }

        // If extra logging then print details passed in
        if (extra_logging) {
            System.out.println("TCPend: TCP Type: " + (tcp_type == TCP_sender ? "Sender" : "Receiver"));
            System.out.println("TCPend: Listening Port: " + listening_port);
            System.out.println("TCPend: Target Port: " + target_port);
            System.out.println("TCPend: Target IP Address: " + TCPStringfromIPv4Address(target_ipAddress));
            System.out.println("TCPend: File Name: " + file_name);
            System.out.println("TCPend: MTU: " + mtu);
            System.out.println("TCPend: Window Size: " + window_size);
        }
    }

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     * Code is taken from IPv4.java, because I am not allowed to directly reference it.
     * @param ipAddress
     * @return
     */
    public int TCPtoIPv4Address(String ipAddress) {
        if (ipAddress == null)
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");
        String[] octets = ipAddress.split("\\.");
        if (octets.length != 4) 
            throw new IllegalArgumentException("Specified IPv4 address must" +
                "contain 4 sets of numerical digits separated by periods");

        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result |= Integer.valueOf(octets[i]) << ((3-i)*8);
        }
        return result;
    }

    /**
     * Accepts an IPv4 address and returns of string of the form xxx.xxx.xxx.xxx
     * ie 192.168.0.1
     * Code is taken from IPv4.java, because I am not allowed to directly reference it.
     * 
     * @param ipAddress
     * @return
     */
    public static String TCPStringfromIPv4Address(int ipAddress) {
        StringBuffer sb = new StringBuffer();
        int result = 0;
        for (int i = 0; i < 4; ++i) {
            result = (ipAddress >> ((3-i)*8)) & 0xff;
            sb.append(Integer.valueOf(result).toString());
            if (i != 3)
                sb.append(".");
        }
        return sb.toString();
    }
}