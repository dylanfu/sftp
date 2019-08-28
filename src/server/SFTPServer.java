package server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import enums.*;

public class SFTPServer {

    // Class member variables
    private AuthManager auth;
    private FileManager fileManager;
    private LoginState state = LoginState.NOT_LOGGED_IN;
    private int loggedInUserID = 0;
    private String loggedInAccount = null;
    private FileType fileType = FileType.BINARY;
    private static final File defaultDirectory = FileSystems.getDefault().getPath("").toFile().getAbsoluteFile();
    private String currentDir = defaultDirectory.toString();
    private String requestedDir = null;
    private String renameFile = null;
    private String fileToSend = null;
    private OutputStream outputStream;
    private StoreType storeType = StoreType.NO_FILE;
    private String storeName = null;
    private long fileLength = 0;
    private final boolean isFileGeneration = true;

    /**
     * SFTPServer Constructor
     * @param port: int
     * @throws Exception
     */
    public SFTPServer(int port) throws Exception {
        String input;
        String command;
        String args;
        String response;
        boolean active = true;

        // Initalise Server Socket Connection
        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        Socket connection = serverSocket.accept();

        // Setup Input and Output Streams
        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        PrintWriter outToClient = new PrintWriter(connection.getOutputStream(),true);
        outputStream = connection.getOutputStream();

        auth = new AuthManager();
        fileManager = new FileManager();

        // Server Loop
        while(active) {

            // Store file
            if (fileLength != 0) {
                boolean saveSuccess;
                // Create and read data into byte array
                byte[] receivedFile = new byte[(int) fileLength];
                for (int i=0; i<fileLength; i++) {
                    receivedFile[i] = (byte) connection.getInputStream().read();
                }

                // Write byte array to a file
                if ((storeType == StoreType.NEW_FILE) || (storeType == StoreType.OVERWRITE_FILE)) {
                    saveSuccess = fileManager.saveFile(storeType,receivedFile,currentDir + "/" + storeName);
                } else if (storeType == StoreType.GENERATION_FILE) {
                    saveSuccess = fileManager.saveFile(storeType,receivedFile,currentDir + "/" + "new_" + storeName);
                } else {
                    saveSuccess = fileManager.saveFile(storeType,receivedFile,currentDir + "/" + storeName);
                }

                if (saveSuccess){
                    // If file save was successful
                    outToClient.println("+Saved " + storeName + "\0");
                    storeName = null;
                    storeType = StoreType.NO_FILE;
                    fileLength = 0;
                } else {
                    // If it fails
                    storeName = null;
                    storeType = StoreType.NO_FILE;
                    fileLength = 0;
                    outToClient.println("-Couldn’t save\0");
                }
            } else {
                input = inFromClient.readLine();
                command = input.substring(0, Math.min(input.length(), 4));

                if (command.equals("DONE")) {
                    active = false;
                    response = "+";
                } else if (command.equals("SEND")) {
                    response = sendCommand();
                } else if (command.equals("STOP")) {
                    response = stopCommand();
                } else {
                    // Get argument parameters
                    try {
                        args = input.substring(5,input.length()-1);
                    } catch (Exception e) {
                        outToClient.println("-Unknown command\0");
                        continue;
                    }

                    switch (command) {
                        case "USER":
                            response = userCommand(args);
                            break;
                        case "ACCT":
                            response = acctCommand(args);
                            break;
                        case "PASS":
                            response = passCommand(args);
                            break;
                        case "TYPE":
                            response = typeCommand(args);
                            break;
                        case "LIST":
                            response = listCommand(args);
                            break;
                        case "CDIR":
                            response = cdirCommand(args);
                            break;
                        case "KILL":
                            response = killCommand(args);
                            break;
                        case "NAME":
                            response = nameCommand(args);
                            break;
                        case "TOBE":
                            response = tobeCommand(args);
                            break;
                        case "RETR":
                            response = retrCommand(args);
                            break;
                        case "STOR":
                            response = storCommand(args);
                            break;
                        case "SIZE":
                            response = sizeCommand(args);
                            break;
                        default:
                            response = "-Unknown command";
                            break;
                    }
                }
                outToClient.println(response + "\0");
            }
            serverSocket.close();
        }
    }

    /**
     * USER Command
     * @param args: String
     * @return response: String
     */
    private String userCommand(String args) {
        int _userID;
        int _status;

        // Get argument parameter
        try {
            _userID = Integer.parseInt(args);
        } catch (Exception e) {
            return "-Invalid user-id, try again";
        }

        // Get User ID
        _status = auth.validateUserID(_userID);

        if (_status == 0) {
            return "-Invalid user-id, try again";
        } else if (_status == 1) {
            state = LoginState.USER_GIVEN;
            loggedInUserID = _userID;
            loggedInAccount = null;
            return "+User-id valid, send account and password";
        } else {
            state = LoginState.LOGGED_IN;
            loggedInUserID = _userID;
            loggedInAccount = null;
            return "!" + _userID + " logged in";
        }
    }

    /**
     * ACCT Command
     * @param args: String
     * @return response: String
     */
    private String acctCommand(String args) {
        int _status;

        // Check if already logged in or userId not specified
        if (state == LoginState.LOGGED_IN) {
            return "!Account valid, logged-in";
        } else if (state == LoginState.NOT_LOGGED_IN) {
            return "-Invalid account, try again";
        }

        // Check account
        _status = auth.validateAccount(loggedInUserID, args);

        if (_status == 0) {
            state = LoginState.USER_GIVEN;
            loggedInAccount = null;
            return "-Invalid account, try again";
        } else if (_status == 1) {
            loggedInAccount = args;
            if (state == LoginState.USER_ACCT_GIVEN) {
                state = LoginState.LOGGED_IN;
                return "!Account valid, logged-in";
            } else {
                state = LoginState.ACCT_GIVEN;
                return "+Account valid, send password";
            }
        } else {
            state = LoginState.LOGGED_IN;
            loggedInAccount = args;
            if (requestedDir != null) {
                String output = cdirCommand(requestedDir);
                requestedDir = null;
                return output;
            }
            return "!Account valid, logged-in";
        }
    }

    /**
     * PASS Command
     * @param args: String
     * @return response: String
     */
    private String passCommand(String args) {
        int _status;

        // If already logged in, no need for password
        if (state == LoginState.LOGGED_IN) {
            return "!Logged in";
        }

        // If not logged in, and no user id has been specified
        if (state == LoginState.NOT_LOGGED_IN) {
            return "-Wrong password, try again";
        }

        // Check login data for that account
        _status = auth.validatePassword(loggedInUserID, loggedInAccount, args);

        // Output result of command
        if (_status == 0) {
            return "-Wrong password, try again";
        } else if (_status == 1) {
            state = LoginState.USER_ACCT_GIVEN;
            return "+Send account";
        } else {
            state = LoginState.LOGGED_IN;
            if (requestedDir != null) {
                String output = cdirCommand(requestedDir);
                requestedDir = null;
                return output;
            }
            return "!Logged in";
        }
    }

    /**
     * TYPE Command
     * @param args: String
     * @return response: String
     */
    private String typeCommand(String args) {
        if (state == LoginState.LOGGED_IN) {
            // Change file transfer type
            if (args.equals("A")) {
                fileType = FileType.ASCII;
                return "+Using Ascii mode";
            } else if (args.equals("B")) {
                fileType = FileType.BINARY;
                return "+Using Binary mode";
            } else if (args.equals("C")) {
                fileType = FileType.CONTINUOUS;
                return "+Using Continuous mode";
            } else {
                return "-Type not valid";
            }
        } else {
            return "-Not logged in, please log in";
        }
    }

    /**
     * LIST Command
     * @param args: String
     * @return response: String
     */
    private String listCommand(String args) {
        String _format;
        String _path;

        if (state == LoginState.LOGGED_IN) {
            // Get argument parameters
            try {
                _format = args.substring(0, 1);
                if (args.length() < 3) {
                    _path = currentDir;
                } else {
                    _path = args.substring(2);
                }
            } catch (Exception e) {
                return "-Format or directory not valid";
            }

            // Get file list of directory
            if (_format.equals("F")) {
                return fileManager.getFileList(_path, "F");
            } else if (_format.equals("V")) {
                return fileManager.getFileList(_path, "V");
            } else {
                return "-Format not valid";
            }
        } else {
            return "-Not logged in, please log in";
        }
    }

    /**
     * CDIR Command
     * @param args: String
     * @return response: String
     */
    private String cdirCommand(String args) {
        if (state == LoginState.NOT_LOGGED_IN)
            return "-Can’t connect to directory because no user-id provided";
        if (!fileManager.checkDirectoryExists(args))
            return "-Can’t connect to directory because directory doesn't exist";
        if (state == LoginState.LOGGED_IN) {
            currentDir = args;
            return "!Changed working dir to " + args;
        } else {
            requestedDir = args;
            return "+Directory ok, send account/password";
        }
    }

    /**
     * KILL Command
     * @param args: String
     * @return response: String
     */
    private String killCommand(String args) {
        if (state == LoginState.LOGGED_IN) {
            if (fileManager.checkFileExists(currentDir + "/" + args)) {
                if (fileManager.deleteFile(currentDir + "/" + args)) {
                    return "+" + args + " deleted";
                } else {
                    return "-Not deleted because of an unknown error";
                }
            } else {
                return "-Not deleted because file doesn't exist";
            }
        } else {
            return "-Not deleted because client is not logged in";
        }
    }

    /**
     * NAME Command
     * @param args: String
     * @return response: String
     */
    private String nameCommand(String args) {
        if (state == LoginState.LOGGED_IN) {
            // Check if file exists
            if (fileManager.checkFileExists(currentDir + "/" + args)) { // Check if file exists
                renameFile = args;
                return "+File exists";
            } else {
                renameFile = null;
                return "-Can’t find " + args;
            }
        } else {
            return "-Not logged in, please log in";
        }
    }

    /**
     * TOBE Command
     * @param args: String
     * @return response: String
     */
    private String tobeCommand(String args) {
        String _currentPath;
        String _newPath;

        if (state == LoginState.LOGGED_IN) {
            // Check name specified
            if (renameFile == null) {
                return "-File wasn't renamed because NAME not specified";
            }

            // Specify current and new file directories
            _currentPath = currentDir + "/" + renameFile;
            _newPath = currentDir + "/" + args;

            // Attempt to rename file
            if (fileManager.renameFile(_currentPath, _newPath)) {
                return "+" + renameFile + " renamed to " + args;
            }
            return "-File wasn't renamed due to an unknown error";
        } else {
            return "-Not logged in, please log in";
        }
    }

    /**
     * RETR Command
     * @param args: String
     * @return response: String
     */
    private String retrCommand(String args) {
        if (state == LoginState.LOGGED_IN) {
            // Check if file exists
            if (fileManager.checkFileExists(currentDir + "/" + args)) {
                fileToSend = args;
                return fileManager.getFileSize(currentDir + "/" + args);
            } else {
                return "-File doesn't exist";
            }
        } else {
            return "-Not logged in, please log in";
        }
    }

    /**
     * SEND Command
     * @return response: String
     */
    private String sendCommand() {
        if (fileToSend != null) {
            // Get the file
            File path = new File(currentDir + "/" + fileToSend);
            try {
                // Convert it to a byte array
                byte[] fileContent = Files.readAllBytes(path.toPath());
                // Write the byte array to the output stream
                outputStream.write(fileContent);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        } else {
            return "-No File Specified";
        }
    }

    /**
     * STOP Command
     * @return response: String
     */
    private String stopCommand() {
        fileToSend = null;
        return "+Ok, RETR aborted";
    }

    /**
     * STOR Command
     * @param args: String
     * @return response: String
     */
    private String storCommand(String args) {
        String _filePath;
        String _type;
        boolean _exists;

        // Check if logged in
        if (state != LoginState.LOGGED_IN) {
            return "-Not logged in, please log in";
        }

        // Get the argument parameters
        try {
            _type = args.substring(0, 3);
            _filePath = args.substring(4);
        } catch (Exception e) {
            return "-Invalid parameters";
        }

        // Checks if file exists
        _exists = fileManager.checkFileExists(currentDir + "/" + _filePath);

        // Gets the specified store type
        if (_type.equals("NEW")) {
            if (_exists) {
                if (isFileGeneration) {
                    storeName = _filePath;
                    storeType = StoreType.GENERATION_FILE;
                    return "+File exists, will create new generation of file";
                } else {
                    storeType = StoreType.NO_FILE;
                    return "-File exists, but system does not support generations";
                }
            } else {
                storeType = StoreType.NEW_FILE;
                storeName = _filePath;
                return "+File does not exist, will create new file";
            }
        } else if (_type.equals("OLD")) {
            storeName = _filePath;
            if (_exists) {
                storeType = StoreType.OVERWRITE_FILE;
                return "+Will write over old file";
            } else {
                storeType = StoreType.NEW_FILE;
                return "+Will create new file";
            }
        } else if (_type.equals("APP")) {
            storeName = _filePath;
            if (_exists) {
                storeType = StoreType.APPEND_FILE;
                return "+Will append to file";
            } else {
                storeType = StoreType.NEW_FILE;
                return "+Will create file";
            }
        } else {
            return "-Invalid parameters";
        }
    }

    /**
     * SIZE Command
     * @param args: String
     * @return response: String
     */
    private String sizeCommand(String args) {
        long _size;

        if (state != LoginState.LOGGED_IN) {
            return "-Not logged in, please log in";
        }

        // Check if file and store type has been specified
        if (storeType == StoreType.NO_FILE) {
            return "-Please specify filename and store type";
        }

        // Get the size
        try {
            _size = Long.parseLong(args);
        } catch (Exception e) {
            // Any errors
            storeType = StoreType.NO_FILE;
            storeName = null;
            return "-Invalid parameters";
        }

        // Check for free space in directory
        if (_size > fileManager.checkFreeSpace(currentDir)) {
            storeType = StoreType.NO_FILE;
            storeName = null;
            return "-Not enough room, don’t send it";
        } else {
            fileLength = _size;
            return "+Ok, waiting for file";
        }
    }

}