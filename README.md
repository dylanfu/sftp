# SFTP Implementation
__Client and Server Implementation using Simple File Transfer Protocol__

## NOTE
* Developed and tested on MacOS with Java 1.8
* Client side saves and sends files local to the main project folder
* Server side saves and sends files to the directory the client specifies
* Ensure only a single space between each argument of the command
* The default server directory is the project directory
* CDIR works for absolute paths only
* LIST with no parameters works for the current directory, but when specifying a file path, please specify an absolute path
* When sending files to the server, ensure that the folder is not protected by administrative privileges
* Additionally, please do not try and send empty files

## Setup and Usage
### Requirements
* `Java 1.8`
* `org.json` Java library

### IntelliJ IDE
* Open the project in IntelliJ
* Add the `json-20190722.jar` library to the project
* Then run the `SFTPRun.main()`

### Jar File
* To run the jar file execute the following in terminal: `java -jar sftp.jar`

### Running Implementation
* Run the project to create both client and server instances
* Type commands in the terminal as specified in the documentation, excluding \<NULL\> characters
* For example, `LIST F /Users/<user>/Documents` to list all the files and folders in the "/Users/\<user\>/Documents", non-verbose
* Press `ENTER` to send the command to the server

* __When using the STOR command, you will have to enter the size of the file yourself with SIZE afterwards by manually checking its size (in bytes)__
* __Before attempting to rerun the program, ensure to use the `DONE` command to safely disconnect the server and client__

## Test Cases
### USER
* `USER 1` should cause the server to reply that account and password should then be entered
* `USER 2` should cause the server to reply that the user is logged in, as this user ID has no corresponding account or password
* `USER 5` should cause the server to reply that the user ID does not exist

### ACCT 
* `USER 1` to specify to the server which user we want to login, then:
* `ACCT admin` to specify to the server which account we want to use - this will cause the server to reply that a password should be entered
* If the password has already been entered (i.e USER 1 => PASS admin => ACCT admin) then the server will reply that the user is logged in
* Incorrect account names will cause the server to reply that the account name does not exist and the account name must be respecified

### PASS
* `USER 1` to specify to the server which user we want to login, then:
* `PASS admin` to specify to the server our user password - this will cause the server to reply that an account name should be entered
* If the account name has already been entered (i.e USER 1 --> ACCT admin --> PASS admin) then the server will reply that the user is logged in
* Incorrect passwords will cause the server to reply that the password is incorrect and must be reentered

### TYPE
* `USER 2` is the quickest way to login
* `TYPE A` or `TYPE B` or `TYPE C` will cause the specified file transfer type to be selected

### LIST
* `USER 2` is the quickest way to login
* `LIST F` will then display the contents of the current directory - which is the project root by default
* `LIST V /Users/<user>/Documents` will display the contents of the Documents folder, with additional data

### CDIR
* `USER 2` to login
* `CDIR /Users` will change the current working directory to /Users
* Use "LIST F" to prove that this has happened

### KILL
* Create a file called "toDelete.txt" in the Documents directory at /Users/\<user\>/Documents
* `USER 2` to login
* `CDIR /Users/<user>/Documents` to change the working directory
* `KILL toDelete.txt` to delete the file
* Use `LIST F` to prove that this has happened

### NAME 
* Create a file called "toRename.txt" in the Documents directory at /Users/\<user\>/Documents
* `USER 2` to login
* `CDIR /Users/<user>/Documents` to change the working directory
* `NAME toRename.txt` to specify the file to rename
* `TOBE renamed.txt` to rename the file
* Use `LIST F` to prove that this has happened

### DONE
* At any time, use this command to safely close the connection
* Server will respond with a "+" and then the connection will be closed

### RETR
* Create a file called toSend.txt in the Documents directory at /Users/\<user\>/Documents, and write some text in it
* `USER 2` to login
* `CDIR /Users/<user>/Documents` to change the working directory
* `RETR toSend.txt` to specify the file to be received by the client - the server will respond with the size of the file in bytes
* `SEND` will cause the file to be sent, or:
* `STOP` will abort the operation
* Check the project folder to see that the file has been copied to there

### STOR
* Create a file called toSend.txt in the project folder, and write some text in it
* `USER 2` to login
* `CDIR /Users/<user>/Documents` to change the working directory
* `STOR NEW toSend.txt` to specify the file to be sent by the client, this will create a new file
* `SIZE X` where X is the size of the file in bytes - manually check the file size (Bytes) in the file explorer
* The file will then be sent, check the Documents directory at /Users/\<user\>/Documents to prove this
* Repeat the STOR and SIZE steps above, which will create a new generation of the toSend.txt file
* If OLD is used, then the toSend.txt file will be overwritten (if it exists)
* If APP is used, then the contents of the toSend.txt that is sent will be appended to the existing file on the server
