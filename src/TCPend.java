import java.io.*;
import java.nio.file.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
    private DatagramSocket socket_in; // Socket for communication in
    private DatagramSocket socket_out; // Socket for communication out

    /************************** Start Main ****************************************/

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

        // Test the read/write file process
        try {
            tcpE.testReadWriteFile(tcpE.file_name);
        } catch (IOException e) {
            System.out.println("Error in file read/write process: " + e.getMessage());
            return;
        }

        // Test the socket connection
        

        

        // Create the datagram packet for sending data, data should be small and printed at destination
        if (tcpE.tcp_type == tcpE.TCP_sender) {

            // Create the socket for sending data
            tcpE.socket_out = tcpE.createSocket(tcpE.listening_port);
            if (tcpE.socket_out == null) {
                System.out.println("Error creating socket for sending data.");
                return;
            }

            System.out.println("Sender mode is active.");

            byte[] data = new byte[1024]; // Example data to send
            // Fill the data with some example content
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) i;
            }
            DatagramPacket packet = tcpE.createPacket(data, 0, data.length, tcpE.target_ipAddress, 8080);
            try {
                tcpE.sendPacket(tcpE.socket_out, packet);
                System.out.println("Packet sent successfully.");
            } catch (IOException e) {
                System.out.println("Error sending packet: " + e.getMessage());
                return;
            }

        } else {
            
            // Create the socket for receiving data
            tcpE.socket_in = tcpE.createSocket(tcpE.target_port);
            if (tcpE.socket_in == null) {
                System.out.println("Error creating socket for receiving data.");
                return;
            }

            System.out.println("Receiver mode is active.");
            byte[] buffer = new byte[1024]; // Buffer for receiving data
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                packet = tcpE.receivePacket(tcpE.socket_in, buffer, buffer.length);
            } catch (IOException e) {
                System.out.println("Error receiving packet: " + e.getMessage());
                return;
            }
            System.out.println("Packet received successfully.");
            System.out.println("Data: " + new String(packet.getData(), 0, packet.getLength()));
            System.out.println("Sender IP: " + packet.getAddress().getHostAddress());
            System.out.println("Sender Port: " + packet.getPort());
            System.out.println("Data Length: " + packet.getLength());
        }
        
        // When running locally on my machine, I can use IP address 127.17.0.1 and any assigned port.

        // Open port with: socat -v UDP-RECV:12345 -
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

    /** -------------------- I might be able to pull from the code directly, waiting on clarification from piazza.
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
     * Create a class that will be the file opened for reading.
     * This class will be used to read the file in chunks, and then return those chunks when requested
     */
    class FileChunkReader {
        private File file;
        private int chunkSize;
        private int currentPosition;

        public FileChunkReader(File file, int chunkSize) {
            this.file = file;
            this.chunkSize = chunkSize;
            this.currentPosition = 0;
        }

        public byte[] readNextChunk() throws IOException {
            // Check if we have reached the end of the file
            if (currentPosition >= file.length()) {
                return null; // No more data to read
            }

            // Calculate the size of the next chunk
            int remainingBytes = (int) (file.length() - currentPosition);
            int bytesToRead = Math.min(chunkSize, remainingBytes);

            // Create a byte array to hold the chunk data
            byte[] chunkData = new byte[bytesToRead];

            // Read the chunk from the file
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.skip(currentPosition); // Skip to the current position
                fis.read(chunkData); // Read the chunk data
            }

            // Update the current position
            currentPosition += bytesToRead;

            return chunkData;
        }
    }

    /**
     * This method will take a file and is for testing purposes, fill evaluate ability to read/write bytes to/from a file.
     * @param filename
     * @return
     * @throws IOException
     */
    private void testReadWriteFile(String filename) throws IOException {
        
        // Create a file object
        File file = new File(filename);
        
        // Create a byte array to hold the data
        byte[] data = new byte[1024]; // 1 KB buffer

        // Keep track of volume of data written
        int bytesWritten = 0;

        // update the file name with a 1 prior to the extension
        String newFileName = filename.substring(0, filename.lastIndexOf('.')) + "_1" + filename.substring(filename.lastIndexOf('.'));
        File newFile = new File(newFileName);
        // Create the new file if it doesn't exist
        this.createFile(newFileName);

        // Loop over file chunk reader until null pulling data
        FileChunkReader fileChunkReader = new FileChunkReader(file, 1024);
        byte[] chunk;
        while ((chunk = fileChunkReader.readNextChunk()) != null) {
            // Write the chunk to the new file
            writeByteArrayToFile(newFile, chunk);
            bytesWritten += chunk.length;
        }
        System.out.println("Read " + bytesWritten + " bytes from file.");
    }

    /**
     * This method will take a byte array and write it to the end of a file.
     * @param file
     * @param data
     * @throws IOException
     */
    private void writeByteArrayToFile(File file, byte[] data) throws IOException {
        // Write the byte array to the end of the file
        try (FileOutputStream fos = new FileOutputStream(file, true)) {
            fos.write(data);
        }
    }

    /******************************** This is the code that will handle sending the packets and creating sockets. *******************************************/
    /**
     * This method will create a socket for sending data.
     * @param port if 0 will assign random port, otherwise attempt with assigned value
     * @return
     */
    private DatagramSocket createSocket(int port) {
        try {
            // Create a new DatagramSocket a random port if port = 0
            if (port == 0) {
                return new DatagramSocket(); // Use a random port
            }
            // Create the socket with the specified port
            return new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Error creating socket: " + e.getMessage());
            return null;
        }
    }
    
    
    /**
     * This method will create a packet for sending data.
     * @param data
     * @param offset
     * @param length
     * @param address
     * @param port
     */
    private DatagramPacket createPacket(byte[] data, int offset, int length, int address, int port) {
        // Create a new DatagramPacket with the specified data and address
        try {
            return new DatagramPacket(data, offset, length, InetAddress.getByName(TCPStringfromIPv4Address(address)), port);
        } catch (UnknownHostException e) {
            System.out.println("Error creating packet: " + e.getMessage());
            return null;
        }
        
    }

    /**
     * This method will send a packet using the socket.
     * @param socket
     * @param packet
     * @throws IOException
     */
    private void sendPacket(DatagramSocket socket, DatagramPacket packet) throws IOException {
        // Send the packet using the socket
        socket.send(packet);
    }

    /**
     * This method will close the socket.
     * @param socket
     */
    private void closeSocket(DatagramSocket socket) {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * This method will listen for incoming packets on the socket.
     * @param socket
     * @param buffer
     * @param length
     * @return
     * @throws IOException
     */
    private DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer, int length) throws IOException {
        // Create a new DatagramPacket to receive the data
        DatagramPacket packet = new DatagramPacket(buffer, length);
        // Receive the packet using the socket
        socket.receive(packet);
        return packet;
    }

}
