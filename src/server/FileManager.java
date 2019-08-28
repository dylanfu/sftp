package server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import enums.StoreType;


public class FileManager {

    /**
     * Retrieves file list of specified directory
     * @param dir: String
     * @param format: String
     * @return fileList: String
     */
    public String getFileList(String dir, String format) {
        String fileList = "";

        File path = new File(dir);
        try {
            File[] files = path.listFiles();
            // Append directory name
            fileList = fileList + "+" + dir + "\r\n";
            for (File file : files) {
                // Append file/folder names
                fileList = fileList + file.getName();
                if (format.equals("V")) {
                    if (!file.isFile()) {
                        // Append "/" if folder
                        fileList = fileList + "/";
                    }
                    // Append size and date
                    fileList = fileList + "\t\t\t Size: " + file.length() +  " B \t\t Last Modified: " + new Date(file.lastModified());
                }
                fileList = fileList + "\r\n";
            }
        } catch (Exception e) {
            if (e.getMessage() == null) {
                return "-Directory path doesn't exist";
            }
            return "-" + e.getMessage();
        }

        return fileList;
    }

    /**
     * Checks if directory exists
     * @param dir: String
     * @return ifDirectoryExists: boolean
     */
    public boolean checkDirectoryExists(String dir) {
        Path path = Paths.get(dir);
        return Files.isDirectory(path);
    }

    /**
     * Checks if file exists
     * @param dir: String
     * @return ifFileExists: boolean
     */
    public boolean checkFileExists(String dir) {
        File path = new File(dir);
        return path.exists();
    }

    /**
     * Deletes a specified file
     * @param dir: String
     * @return ifDeleted: boolean
     */
    public boolean deleteFile(String dir) {
        File file = new File(dir);
        return file.delete();
    }

    /**
     * Renames a specified file
     * @param currentName: String
     * @param newName: String
     * @return ifRenamed: boolean
     */
    public boolean renameFile(String currentName, String newName) {
        File destination = new File(newName);
        if (destination.exists()) {
            return false;
        } else {
            File source = new File(currentName);
            if (source.renameTo(destination)) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Retrieves the size of a specified file
     * @param dir: String
     * @return size: String
     */
    public String getFileSize(String dir) {
        File file = new File(dir);
        return Integer.toString((int)file.length());
    }

    /**
     * Checks if there is free space in specified directory
     * @param dir: String
     * @return spaceAvailable: long
     */
    public long checkFreeSpace(String dir) {
        File file = new File(dir);
        return file.getFreeSpace();
    }

    /**
     * Saves a file at a specified directory
     * @param storeType: StoreType
     * @param receivedFile: byte[]
     * @param path: String
     * @return ifSaved: boolean
     */
    public boolean saveFile(StoreType storeType, byte[] receivedFile, String path) {
        try{
            // Write byte array to a file at specified path
            if ((storeType == StoreType.NEW_FILE) || (storeType == StoreType.OVERWRITE_FILE)) {
                FileOutputStream stream = new FileOutputStream(path);
                stream.write(receivedFile);
                stream.close();
            } else if (storeType == StoreType.GENERATION_FILE) {
                FileOutputStream stream = new FileOutputStream(path);
                stream.write(receivedFile);
                stream.close();
            } else {
                FileOutputStream stream = new FileOutputStream(path, true);
                stream.write(receivedFile);
                stream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }

}