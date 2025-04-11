import java.io.*;
import java.nio.file.*;

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

        // Check if the file or folder exists
        if (!tcpE.startupVerifyFolderFile(tcpE.file_name)) {
            System.out.println("Error in file verification process.");
            return;
        }

    }

    /************************************ Code to handle initial startup and parsing of arguments. *********************************************/
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

    /******************************************** Code to handle file management. **************************************************/
    
    /**
     * This method will be called from the main method to check if the file or folder exists.
     * @param fileName
     * @return 
     */
    private boolean startupVerifyFolderFile(String fileName) {
        File file = new File(fileName);
        if (this.tcp_type == TCP_sender) {
            if (checkFileExists(file)) {
                System.out.println("The file " + fileName + " exists, and will be read.");
                return true;
            } else {
                System.out.println("The file " + fileName + " does not exist.");
                return false;
            }
        }
        else if (this.tcp_type == TCP_receiver) {
            if (checkFileExists(file)) {
                System.out.println("The file " + fileName + " already exists, program will not overwrite data.");
                return false;
            } 
            else {
                // Create the file if it doesn't exist for the receiver
                try {
                    if (this.createFile(fileName)) {
                        System.out.println("The file " + fileName + " was created.");
                        return true;
                    } else {
                        System.out.println("The file " + fileName + " already exists.");
                        return false;
                    }
                } catch (IOException e) {
                    System.out.println("Error creating file " + fileName + ": " + e.getMessage());
                    return false;
                }
            }
        }
        return false; // Something unexpected happened if we  reach here
    }
    
    /**
     * This method will take a foldername and confirm that the folder exists.
     * @param folder
     * @return
     */
    private boolean checkFolderExists(File folder) {
        return (boolean) folder.exists() && folder.isDirectory();
    }

    /**
     * This method will take a filename and confirm that the file exists.
     * @param filePath
     * @return
     */
    private boolean checkFileExists(File file) {
        return (boolean) file.exists() && file.isFile();
    }

    /**
     * If a filepath doesn't exist, create it.
     * @param filePath
     * @return true if file was created, false if it already existed or failed to create.
     * @throws IOException if an error occurs while creating the file.
     */
    private boolean createFile(String filePath) throws IOException {
        File file = new File(filePath);
        // Create directories if they don't exist
        File parentDir = file.getParentFile();

        // Check if the parent directory exists, if not create it
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // Create parent directories if they don't exist
        }

        // Create the file if it doesn't exist
        if (!file.exists()) {
            return file.createNewFile();
        }
        return false;
    
    }  

    /********************** Code to handle data extraction from a file or insertion into a file.*********************************** */

    /**
     * For the sending side, I need to keep data in the buffer until it is confirmed that it was received - then I can drop it
     * I will have a temp_buffer that a copy of is added to the end of my real_buffer. Once I have confirmation I can drop it from the list of buffer chunks.
     * I'll load as much as I can into the outbound buffer, and then send it what I am permitted to send.
     * I'll have to periodically check if there is space to read things into the buffer.
     */

    /**
     * This method will take a file and read the data from it.
     * @param file
     * @return
     */
}