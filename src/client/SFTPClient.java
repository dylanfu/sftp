package client;

import java.io.*;
import java.net.*;

public class SFTPClient {

    public SFTPClient(int port) throws Exception {
        String toServer;
        String fromServer;
        boolean active = true;

        Socket connection = new Socket("localhost", port);
        connection.setReuseAddress(true);
        connection.setKeepAlive(true);

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        PrintWriter outToServer = new PrintWriter(connection.getOutputStream(),true);

        while(active) {
            System.out.print("COMMAND: ");

            toServer = inFromUser.readLine();
            outToServer.println(toServer + '\0');
            fromServer = inFromServer.readLine();

            System.out.println("RESPONSE: " + fromServer);

            if (toServer.equals("DONE")) {
                active = false;
            }
        }

        connection.close();
    }
}