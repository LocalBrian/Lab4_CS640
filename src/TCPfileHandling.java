import java.io.*;
import java.nio.file.*;

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
public class TCPfileHandling {

    private File file;
    private String filePath;
    private int chunkSize;
    private int currentPosition;
    public int currentChunkSize; // Size of the current chunk read
    public int totalData;

    public TCPfileHandling(String filePath, int chunkSize) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.chunkSize = chunkSize;
        this.currentPosition = 0;
        this.totalData = 0;
    }

    public TCPfileHandling(String filePath) {
        this.filePath = filePath;
        this.file = new File(filePath);
        this.currentPosition = 0;
        this.totalData = 0;
    }

    public byte[] readNextChunk() throws IOException {
        // Check if we have reached the end of the file
        if (!hasNextChunk()) {
            System.out.println("End of file reached.");
            return null; // No more data to read
        }

        // Calculate the size of the next chunk
        int remainingBytes = (int) (file.length() - currentPosition);
        int bytesToRead = Math.min(chunkSize, remainingBytes);

        this.totalData += bytesToRead;

        // Create a byte array to hold the chunk data
        byte[] chunkData = new byte[bytesToRead];

        // Read the chunk from the file
        try (FileInputStream fis = new FileInputStream(this.file)) {
            fis.skip(currentPosition); // Skip to the current position
            fis.read(chunkData); // Read the chunk data
        }

        // Update the current position
        currentPosition += bytesToRead;
        this.currentChunkSize = bytesToRead;

        return chunkData;
    }

    public boolean hasNextChunk() {
        return currentPosition < this.file.length();
    }

    public void setMaxChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }
    public int getMaxChunkSize() {
        return this.chunkSize;
    }
    public int getCurrentChunkSize() {
        return this.currentChunkSize;
    }
    public int getCurrentPosition() {
        return this.currentPosition;
    }


    /**
     * This method will take a byte array and write it to the end of a file.
     * 
     * @param data
     * @param dataLength
     * @throws IOException
     */
    public void writeByteArrayToFile(byte[] data, int dataLength) throws IOException {
        
        // Working data is data without first 24 bytes
        byte[] workingData = new byte[dataLength];
        System.arraycopy(data, 24, workingData, 0, dataLength);

        this.totalData += workingData.length;
        
        // Write the byte array to the end of the file
        try (FileOutputStream fos = new FileOutputStream(this.file, true)) {
            fos.write(workingData);
            fos.flush();
        }
    }

    /******************************************** Code to handle file management. **************************************************/
    
    /**
     * This method will be called from the main method to check if the file or folder exists.
     * @param wantToCreate
     * @return 
     */
    public boolean startupVerifyFolderFile(boolean wantToCreate) {
        
        if (wantToCreate) {
            if (checkFileExists(this.file)) {
                System.out.println("The file " + this.filePath + " already exists, program will not overwrite data.");
                return false;
            } 
            else {
                // Create the file if it doesn't exist for the receiver
                try {
                    if (this.createFile(this.filePath)) {
                        System.out.println("The file " + this.filePath + " was created.");
                        return true;
                    } else {
                        System.out.println("The file " + this.filePath + " already exists.");
                        return false;
                    }
                } catch (IOException e) {
                    System.out.println("Error creating file " + this.filePath + ": " + e.getMessage());
                    return false;
                }
            }
        }
        else {
            if (checkFileExists(this.file)) {
                System.out.println("The file " + this.filePath + " exists, and will be read.");
                return true;
            } else {
                System.out.println("The file " + this.filePath + " does not exist.");
                return false;
            }
        }

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

    /***************************** Code for testing below *************************************************/
    /**
     * This method will take a file and is for testing purposes, fill evaluate ability to read/write bytes to/from a file.
     * @param filename
     * @return
     * @throws IOException
     */
    public void testReadWriteFile(String filename) throws IOException {
        
        // Create a new object
        TCPfileHandling sourcefileHandler = new TCPfileHandling(filename, 1024);
        
        // Create a byte array to hold the data
        byte[] data = new byte[1024]; // 1 KB buffer

        // Keep track of volume of data written
        int bytesWritten = 0;

        // update the file name with a 1 prior to the extension
        String newFileName = filename.substring(0, filename.lastIndexOf('.')) + "_1" + filename.substring(filename.lastIndexOf('.'));
        // Loop over file chunk reader until null pulling data
        TCPfileHandling targetfileWriter = new TCPfileHandling(newFileName, 1024);
        // Create the new file if it doesn't exist
        targetfileWriter.createFile(newFileName);
        
        byte[] chunk;
        while ((chunk = targetfileWriter.readNextChunk()) != null) {
            // Write the chunk to the new file
            targetfileWriter.writeByteArrayToFile(chunk, chunk.length);
            bytesWritten += chunk.length;
        }
        System.out.println("Read " + bytesWritten + " bytes from file.");
    }
}