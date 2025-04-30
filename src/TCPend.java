import java.io.*;
import java.nio.file.*;


public class TCPend {

    // Flags for which end of TCP this is
    private byte TCP_sender = 1;
    private byte TCP_receiver = 2;
    private byte tcp_type;

    // Arguments that can be passed in 
    private String file_name; // Filename that will be used
    private short communicationPort; // Port that will be used for communication on this host
    private short targetPort; // Port at remote host that will be targetted
    private String targetIPAddress; // IP address to communicate to
    private int maxUnitSize; // Maximum Transmission Unit in bytes
    private int maxUnitCount; // Maximumliding window size in number of segments 

    // Other Attributes
    private boolean extra_logging = true; // Change this flag based on level of logging needed

    /************************** Start Main ****************************************/

    public static void main(String[] args) {
        System.out.println("Executing TCPend on host.");

        // Create an instance of TCPend and TCPconnection
        TCPend tcpE = new TCPend();

        // Verify there was an appropriate amount of arguments sent
        if (args.length != 12) {
            if (args.length != 8) {
                System.out.println("An incorrect number of arguements was sent for a sender or receiver.");
                return;
            }
        }

        // Parse the arguments and perform validation
        if (tcpE.parseArgs(args) == false) {
            System.out.println("The arguements passed were incorrect. Exiting.");
            return;
        }

        // Perform file handling and validation
        TCPfileHandling fileChunker = tcpE.createFileChunker();
        if (fileChunker == null) {
            System.out.println("File chunker could not be created. Exiting.");
            return;
        }

        // Create the instance of the TCPconnection class
        TCPconnection tcpConnect = tcpE.createConnection(fileChunker);
        if (tcpConnect == null) {
            System.out.println("TCP connection could not be created. Exiting.");
            return;
        }

        // Perform communication
        tcpConnect.performTCPcommunication();

        // Close the program
        System.out.println("TCPend execution completed.");
        return;

    }  


    /************************************ Code to handle initial startup and parsing of arguments. *********************************************/
    private boolean parseArgs(String[] args) {
        
        // Default to receiver, will update if argument for sender is received
        tcp_type = TCP_receiver;

        // Parse arguments
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			if (arg.equals("-p"))
			{ this.communicationPort = Short.parseShort(args[++i]); }
			else if(arg.equals("-s"))
            { this.targetIPAddress = args[++i];
              tcp_type = TCP_sender; }
			else if (arg.equals("-a"))
			{ this.targetPort = Short.parseShort(args[++i]); }
			else if (arg.equals("-f"))
			{ this.file_name = args[++i]; }
			else if (arg.equals("-m"))
			{ this.maxUnitSize = Integer.parseInt(args[++i]); }
			else if (arg.equals("-c"))
			{ this.maxUnitCount = Integer.parseInt(args[++i]); }
		}

        // Verify that the needed arguments for Listener or sender were given.
        if (tcp_type == TCP_sender) {
            if (communicationPort == 0 || targetIPAddress == "" || targetPort == 0 || file_name == null || maxUnitSize == 0 || maxUnitCount == 0) {
                System.out.println("An argument was missing for the sender.");
                return false;
            }
        } else {
            if (communicationPort == 0 || file_name == null || maxUnitSize == 0 || maxUnitCount == 0) {
                System.out.println("An argument was missing for the receiver.");
                return false;
            }
        }

        // If extra logging then print details passed in
        if (extra_logging) {
            System.out.println("TCPend: TCP Type: " + (tcp_type == TCP_sender ? "Sender" : "Receiver"));
            System.out.println("TCPend: Listening Port: " + communicationPort);
            System.out.println("TCPend: Target Port: " + targetPort);
            System.out.println("TCPend: Target IP Address: " + targetIPAddress);
            System.out.println("TCPend: File Name: " + file_name);
            System.out.println("TCPend: MTU: " + maxUnitSize);
            System.out.println("TCPend: Window Size: " + maxUnitCount);
        }

        return true;

    }

    /******************************************** Code to handle intializations. ********************************************************/

    /**
     * Create file chunk reader and writer
     * @param file_name
     * @param mtu
     */

    public TCPfileHandling createFileChunker(){
        
        if (this.tcp_type == this.TCP_sender) {
            // Create a file handler for the sender
            TCPfileHandling fileChunkReader = new TCPfileHandling(this.file_name, this.maxUnitSize);
            // Verify the file exists
            if (!fileChunkReader.startupVerifyFolderFile(false)) {
                System.out.println("File does not exist.");
                return null;
            }
            // Return the file chunk reader
            return fileChunkReader;
        } 
            
        // Create a file handler for the receiver
        TCPfileHandling fileChunkWriter = new TCPfileHandling(this.file_name);
        // Verify the file exists
        if (!fileChunkWriter.startupVerifyFolderFile(true)) {
            System.out.println("File could not be created.");
            return null;
        }
        // Return the file chunk writer
        return fileChunkWriter;
    }

    /**
     * Create the connection instances
     */
    public TCPconnection createConnection(TCPfileHandling fileChunkInstance) {
        if (this.tcp_type == this.TCP_sender) {
            // Create a connection for the sender
            TCPconnection tcpConnect = new TCPconnection(fileChunkInstance, this.targetIPAddress, this.communicationPort, this.targetPort, this.maxUnitSize, this.maxUnitCount);
            return tcpConnect;
        }
        // Create a connection for the receiver
        TCPconnection tcpConnect = new TCPconnection(fileChunkInstance, this.communicationPort, this.maxUnitSize, this.maxUnitCount);
        return tcpConnect;
    }


    /****************************** Below code is for conversions of the IP Address *****************************************************/

    /**
     * Accepts an IPv4 address of the form xxx.xxx.xxx.xxx, ie 192.168.0.1 and
     * returns the corresponding 32 bit integer.
     * Code is taken from IPv4.java, from class.
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
     * Code is taken from IPv4.java, from class.
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