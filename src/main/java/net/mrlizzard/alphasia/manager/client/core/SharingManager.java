package net.mrlizzard.alphasia.manager.client.core;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;
import net.mrlizzard.alphasia.manager.client.utils.logger.ChatColor;
import org.apache.commons.net.ftp.*;
import org.apache.log4j.Level;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class SharingManager {

    private static boolean          canStartInstance;
    public static String            prefix                  = ChatColor.BLUE + "# - SHARRING - # " + ChatColor.RESET;

    public SharingManager() {
        canStartInstance = false;
        go(true);
    }

    @SuppressWarnings("unchecked")
    public static void go(boolean display) {
        if(AlphaManagerClient.getInstance().getIpAddress().equals("127.0.0.1"))
            return;

        canStartInstance = false;
        File packages = new File(System.getProperty("java.class.path"));

        Runnable runnable = () -> {
            FTPClient ftpClient = new FTPClient();
            Map<String, String> config = ((Map<String, String>) AlphaManagerClient.getInstance().getClientConfiguration().get("ftp"));

            try {
                ftpClient.connect(config.get("hostname"), Integer.parseInt(config.get("port")));
                int reply = ftpClient.getReplyCode();
                ftpClient.enterLocalPassiveMode();

                if(FTPReply.isPositiveCompletion(reply)) {
                    boolean successLogin = ftpClient.login(config.get("username"), config.get("password"));

                    if(!successLogin) {
                        if(display)
                            AlphaManagerClient.log(Level.ERROR, prefix + "Invalid credentials. Package update ignored.");

                        return;
                    }

                    if(display)
                        AlphaManagerClient.log(Level.INFO, prefix + "Connected to the FTP server. Trying to update default packages files.");

                    // Start updates
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                    listDirectory(ftpClient, config.get("defaultLocation"), "", 1);

                    ArrayList<File> files = listf(packages.getAbsoluteFile().getParentFile().toString() + File.separator + "packages" + File.separator);

                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                String path 	= file.getPath().replace(packages.getAbsoluteFile().getParentFile().toString() + File.separator + "packages" + File.separator, "");
                                FTPFile ffile 	= ftpClient.mlistFile(path);

                                if (ffile == null) {
                                    // File doesn't exist, delete
                                    file.delete();
                                    AlphaManagerClient.log(Level.INFO, prefix + "Deleting file (" + file.getPath() + ").");
                                }
                            } else if (file.isDirectory()) {
                                String path 		= file.getPath().replace(packages.getAbsoluteFile().getParentFile().toString() + File.separator + "packages" + File.separator, "");
                                FTPFile[] ffile 	= ftpClient.mlistDir(path);

                                if (ffile == null) {
                                    // Directory doesn't exist, delete
                                    file.delete();
                                    AlphaManagerClient.log(Level.INFO, prefix + "Deleting directory (" + file.getPath() + ").");
                                } else{
                                    boolean f = false;

                                    for (FTPFile ftpFile : ffile)
                                        f = true;

                                    if (!f) {
                                        // Directory doesn't exist, delete
                                        file.delete();
                                        AlphaManagerClient.log(Level.INFO, prefix + "Deleting directory (" + file.getPath() + ").");
                                    }
                                }
                            }
                        }
                    } else {
                        AlphaManagerClient.log(Level.INFO, prefix + "Remote packages files empty.");
                    }

                    files.clear();
                    files = null;
                    //disconnect ?
                } else {
                    AlphaManagerClient.log(Level.ERROR, prefix + "Unable to connect FTP server (" + reply + ")");
                    System.exit(-1);
                }
            } catch(IOException error) {
                AlphaManagerClient.log(Level.FATAL, error.getMessage() + " => " + error.getCause().getMessage());
                System.exit(-1);
            } finally {
                try {
                    if(ftpClient.isConnected()) {
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }
                } catch(IOException error) {
                    AlphaManagerClient.log(Level.FATAL, prefix + "Unable to logout/disconnect FTP server: " + error.getMessage());
                }
            }

            AlphaManagerClient.log(Level.INFO, prefix + "All packages files up to date !");
            canStartInstance = true;
        };

        new Thread(runnable).start();
    }

    private static void listDirectory(FTPClient ftpClient, String parentDir, String currentDir, int level) throws IOException {
        String dirToList = parentDir;
        File packages = new File(System.getProperty("java.class.path"));

        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = ftpClient.listFiles(dirToList);

        if (subFiles != null && subFiles.length > 0) {
            for (FTPFile aFile : subFiles) {
                String currentFileName = aFile.getName();

                if (currentFileName.equals(".") || currentFileName.equals("..")) {
                    // skip parent directory and directory itself
                    continue;
                }

                if (aFile.isDirectory()) {
                    listDirectory(ftpClient, dirToList, currentFileName, level + 1);
                } else {
                    File file = new File(packages.getAbsoluteFile().getParentFile().toString() + File.separator + "packages" + dirToList + File.separator + currentFileName);

                    if (!file.exists()) {
                        try {
                            file.createNewFile();
                        } catch(Exception error) {
                            String b = file.getPath().replace(currentFileName, "");
                            new File(b).mkdirs();
                            new File(b).mkdir();
                            file.getParentFile().mkdirs();
                            file.createNewFile();
                        }

                        OutputStream outputStream1 = new BufferedOutputStream(new FileOutputStream(file));
                        boolean success = ftpClient.retrieveFile(dirToList + "/" + currentFileName, outputStream1);
                        outputStream1.close();

                        if (aFile.getTimestamp().getTimeInMillis() + 60000L > System.currentTimeMillis()) continue;

                        if (success) {
                            AlphaManagerClient.log(Level.INFO, prefix + "Create file " + file.getPath() + " (" + file.length() + " bytes).");
                        } else {
                            AlphaManagerClient.log(Level.INFO, prefix + "Error on creating file (" + file.getPath() + ").");
                        }
                    } else {
                        if (file.length() != aFile.getSize()) {
                            file.delete();
                            long from 						= file.length();
                            long to 						= aFile.getSize();
                            OutputStream outputStream1 	    = new BufferedOutputStream(new FileOutputStream(file));
                            boolean success 				= ftpClient.retrieveFile(dirToList + "/" + currentFileName, outputStream1);

                            if (aFile.getTimestamp().getTimeInMillis() + 60000L > System.currentTimeMillis()) continue;

                            if (success) {
                                AlphaManagerClient.log(Level.INFO, prefix + "Edit file " + file.getPath() + " (" + from + " => " + to + " bytes)");
                            } else {
                                AlphaManagerClient.log(Level.INFO, prefix + "Error on editing file " + file.getPath() + " (" + from + " => " + to + " bytes)");
                            }

                            outputStream1.close();
                        }
                    }
                }
            }
        }

        subFiles = null;
    }

    private static ArrayList<File> listf(String directoryName) throws NullPointerException {
        ArrayList<File> files 	= new ArrayList<>();
        File directory 		    = new File(directoryName);
        File[] fList 			= directory.listFiles();

        try {
            for (File file : fList) {
                if (file.isFile()) {
                    files.add(file);
                } else if (file.isDirectory()) {
                    files.add(file);

                    for (File fo : listf(file.getAbsolutePath()))
                        files.add(fo);
                }
            }

            fList = null;
            return files;
        } catch(Exception error) {
            AlphaManagerClient.log(Level.ERROR, prefix + error.getMessage());
            return null;
        }
    }

}
