package server;

import java.io.*;
import java.net.*;

public class SFTPServer {

    private Authentication auth;
    private int loginState = 0; // 0 = not logged in, 1 = logged in, 2 = supplied user id, 3 = supplied account name, 4 = password correct, but no account name
    private int loggedInUserID = 0;
    private String loggedInAccount = null;
    private int fileType = 1; // 0 = Ascii, 1 = Binary, 2 = Continuous
    private String currentDir = "/";
    private String requestedDir = null;
    private String renameFile = null;

    public SFTPServer(int port) throws Exception {
        String clientInput;
        String command;
        String args;
        String response;
        boolean active = true;

        ServerSocket serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        Socket connection = serverSocket.accept();

        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        PrintWriter outToClient = new PrintWriter(connection.getOutputStream(),true);

        auth = new Authentication();

        while(active) {

            clientInput = inFromClient.readLine();
            command = clientInput.substring(0, Math.min(clientInput.length(), 4));

            if (command.equals("DONE")) {
                active = false;
                response = "+";
            } else {
                try {
                    args = clientInput.substring(5,clientInput.length()-1);
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
                        response = "TODO";
                        break;
                    case "CDIR":
                        response = "TODO";
                        break;
                    case "KILL":
                        response = "TODO";
                        break;
                    case "NAME":
                        response = "TODO";
                        break;
                    case "TOBE":
                        response = "TODO";
                        break;
                    case "RETR":
                        response = "TODO";
                        break;
                    case "STOR":
                        response = "TODO";
                        break;
                    case "SIZE":
                        response = "TODO";
                        break;
                    default:
                        response = "-Unknown command";
                        break;
                }
            }

            outToClient.println(response + "\0");

            serverSocket.close();
        }


    }

    public String userCommand(String args) {
        int _userID;
        int _status;

        try {
            _userID = Integer.parseInt(args);
        } catch (Exception e) {
            return "-Invalid user-id, try again";
        }

        _status = auth.validateUserID(_userID);

        if (_status == 0) {
            return "-Invalid user-id, try again";
        } else if (_status == 1) {
            loginState = 2;
            loggedInUserID = _userID;
            loggedInAccount = null;
            return "+User-id valid, send account and password";
        } else {
            loginState = 1;
            loggedInUserID = _userID;
            loggedInAccount = null;
            return "!" + _userID + " logged in";
        }
    }

    public String acctCommand(String args) {
        int _status;

        // Check if already logged in or userId not specified
        if (loginState == 1) {
            return "!Account valid, logged-in";
        } else if (loginState == 0) {
            return "-Invalid account, try again";
        }

        _status = auth.validateAccount(loggedInUserID, args);

        if (_status == 0) {
            loginState = 2;
            loggedInAccount = null;
            return "-Invalid account, try again";
        } else if (_status == 1) {
            loggedInAccount = args;
            if (loginState == 4) {
                loginState = 1;
                return "!Account valid, logged-in";
            } else {
                loginState = 3;
                return "+Account valid, send password";
            }
        } else {
            loginState = 1;
            loggedInAccount = args;
//            if (requestedDir != null) {
//                String output = CDIRCommand(requestedDir);
//                requestedDir = null;
//                return output;
//            }
            return "!Account valid, logged-in";
        }
    }

    public String passCommand(String args) {
        int _status;

        // If already logged in, no need for password
        if (loginState == 1) {
            return "!Logged in";
        }

        // If not logged in, and no user id has been specified
        if (loginState == 0) {
            return "-Wrong password, try again";
        }

        // Check login data for that account
        _status = auth.validatePassword(loggedInUserID, loggedInAccount, args);

        // Output result of command
        if (_status == 0) {
            return "-Wrong password, try again";
        } else if (_status == 1) {
            loginState = 4;
            return "+Send account";
        } else {
            loginState = 1;
//            if (requestedDir != null) {
//                String output = CDIRCommand(requestedDir);
//                requestedDir = null;
//                return output;
//            }
            return "!Logged in";
        }
    }

    public String typeCommand(String args) {
        if (loginState == 1) {
            if (args.equals("A")) {
                fileType = 0;
                return "+Using Ascii mode";
            } else if (args.equals("B")) {
                fileType = 1;
                return "+Using Binary mode";
            } else if (args.equals("C")) {
                fileType = 2;
                return "+Using Continuous mode";
            } else {
                return "-Type not valid";
            }
        } else {
            return "-Not Logged In";
        }
    }
}