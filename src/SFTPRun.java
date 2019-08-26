public class SFTPRun {

    public static void main(String argv[]) {
        int port = 115;

        Thread server_thread = new Thread(){
            public void run(){
                System.out.println("SFTP Server Starting");
                try {
                    SFTPServer sftpServer = new SFTPServer(port);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        server_thread.start();

        System.out.println("SFTP Client Starting");
        try {
            SFTPClient sftpClient = new SFTPClient(port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("SFTP Protocol Ended");
    }
}