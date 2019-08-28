package client;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import enums.FileType;

public class SFTPClient {

    /**
     * SFTPClient Constructor
     * @param port: int
     * @throws Exception
     */
    public SFTPClient(int port) throws Exception {

        String toServer;
        String fromServer;
        String command;
        FileType fileType = FileType.BINARY;
        int fileLength = 0;
        String fileName = null;
        String fileToSend = null;
        boolean noFile = false;
        boolean active = true;

        // Initialise client socket connection
        Socket connection = new Socket("localhost", port);
        connection.setReuseAddress(true);
        connection.setKeepAlive(true);

        // Setup input and output streams
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        PrintWriter outToServer = new PrintWriter(connection.getOutputStream(),true);
        OutputStream outputStream = connection.getOutputStream();

        while(active) {
            System.out.print("\nCOMMAND: ");
            toServer = inFromUser.readLine();

            // Get the command from user input
            command = toServer.substring(0, Math.min(toServer.length(), 4));

            // Abort if SIZE command but the file doesn't exist
            if (command.equals("SIZE") && noFile) {
                System.out.println("File doesn't exist on host system. Aborting command.");
                continue;
            }

            // Send command to server
            outToServer.println(toServer + '\0');

            // Receive a file
            if (command.equals("SEND") && fileName != null) {
                byte[] receivedFile = new byte[fileLength];
                for (int i=0; i<fileLength; i++) {
                    receivedFile[i] = (byte) connection.getInputStream().read();
                }
                FileOutputStream stream = new FileOutputStream(fileName);
                try {
                    stream.write(receivedFile);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    stream.close();
                }
                System.out.println(fileName + " received");
                inFromServer.readLine();
                fileName = null;
                continue;
            } else if (command.equals("STOR")) {
                // Prepare to send a file
                try {
                    fileToSend = toServer.substring(9);
                    File file = new File(fileToSend);
                    if (!file.exists()) {
                        noFile = true;
                    } else {
                        noFile = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // Retrieve server response
            fromServer = processResponse(inFromServer);

            // Print server response regarding files
            System.out.println("RESPONSE: " + fromServer);

            // Check file name and length
            if (command.equals("RETR")) {
                try {
                    fileName = toServer.substring(5);
                    fileLength = Integer.parseInt(fromServer.substring(0,fromServer.length()-1));
                } catch (Exception e) {
                    fileName = null;
                    continue;
                }
            }

            // Respond to successful server responses
            if (fromServer.substring(0, 1).equals("+")) {
                if (command.equals("DONE")) {
                    // Close connection
                    active = false;
                } else if (command.equals("STOP")) {
                    continue;
                } else if (command.equals("SIZE")) {
                    // Send file
                    File path = new File(fileToSend);
                    try {
                        byte[] fileContent = Files.readAllBytes(path.toPath());
                        outputStream.write(fileContent);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileToSend = null;

                    // Retrieve server response
                    fromServer = processResponse(inFromServer);

                    // Print server response
                    System.out.println("RESPONSE: " + fromServer);

                } else if (command.equals("TYPE")) {
                    String args = toServer.substring(5);
                    if (args.equals("A")) { fileType = FileType.ASCII; }
                    else if (args.equals("B")) { fileType = FileType.BINARY; }
                    else if (args.equals("C")) {fileType = FileType.CONTINUOUS; }
                }
            }
        }
        connection.close();
    }

    /**
     * Formats and processes server responses
     * @param in: BufferedReader
     * @return void
     * @throws Exception
     */
    private static String processResponse(BufferedReader in) throws Exception {
        StringBuilder builder = new StringBuilder();
        int i;

        while (true) {
            i = in.read();
            builder.append((char) i);
            if (i == 0) {
                in.readLine();
                break;
            }
        }

        return builder.toString();
    }

}