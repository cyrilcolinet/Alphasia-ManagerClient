package net.mrlizzard.alphasia.manager.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.mrlizzard.alphasia.manager.client.core.SharingManager;
import net.mrlizzard.alphasia.manager.client.core.database.CachingConnector;
import net.mrlizzard.alphasia.manager.client.core.database.Publisher;
import net.mrlizzard.alphasia.manager.client.core.database.SingleCacheConnector;
import net.mrlizzard.alphasia.manager.client.core.database.networking.AlphaManagerClientNetwork;
import net.mrlizzard.alphasia.manager.client.core.database.networking.TasksExecutor;
import net.mrlizzard.alphasia.manager.client.utils.logger.ChatColor;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.util.Map;

public class AlphaManagerClient {

    private static AlphaManagerClient                       instance;

    private Gson                                            gson;
    private boolean                                         windowsOs;
    private Logger                                          logger;
    private String                                          ipAddress;
    private String                                          hostname;
    private Map<String, Object>                             clientConfiguration;

    private CachingConnector                                cacheConnector;
    private Publisher                                       publisher;
    private TasksExecutor                                   tasksExecutor;
    private AlphaManagerClientNetwork                       alphaManagerClientNetwork;

    public AlphaManagerClient() {
        instance = this;
        gson = new GsonBuilder().setPrettyPrinting().create();
        windowsOs = System.getProperty("os.name").toLowerCase().contains("win");
        logger = Logger.getLogger(AlphaManagerClient.class);

        File logsFile = new File("logs");

        if(!logsFile.exists())
            logsFile.mkdir();

        this.displayHeader();

        if(System.getProperty("user.name").equalsIgnoreCase("root"))
            log(Level.WARN, "It is highly discouraged to execute this program in super-user.");

        log(Level.INFO, "Loading AlphaManager Client... Please wait.");

        File configFolder = new File("configurations");

        this.checkConfigurations(configFolder);
        this.synchronizeIpAddress();
        this.loadConfigurations(configFolder);
        this.databaseConnection();

        new SharingManager();

        // After CTRL+C, close command, or SystemExit
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                this.onDisable();
            } catch(FileNotFoundException | UnsupportedEncodingException error) {
                log(Level.FATAL, error.getMessage());
            }
        }));
    }

    /**
     * Get IP address from address
     */
    private void synchronizeIpAddress() {
        if(ipAddress == null || ipAddress.isEmpty()) {
            try {
                URL getIP = new URL("http://checkip.amazonaws.com");
                BufferedReader in = new BufferedReader(new InputStreamReader(getIP.openStream()));
                ipAddress = in.readLine();
                hostname = InetAddress.getLocalHost().getHostName();
                log(Level.INFO, "External IP address is " + ipAddress + " and hostname is " + hostname);
            } catch(IOException error) {
                ipAddress = null;
                hostname = null;
                logger.log(Level.ERROR, "Unable to get external ip address: " + error.getMessage());
                System.exit(-1);
            }
        } else {
            ipAddress = null;
        }
    }

    /**
     * Check files configuration.
     */
    private void checkConfigurations(File configFolder) {
        log(Level.INFO, "Check configuration files.");

        if(!configFolder.exists()) configFolder.mkdir();
        else if(!configFolder.isDirectory()) {
            try {
                log(Level.ERROR, "'configurations' is not a directory, please make it as directory.");
                System.exit(-1);
                return;
            } catch(Exception error) {
                log(Level.FATAL, error.getMessage());
                System.exit(-1);
                return;
            }
        }

        File connectorFile = new File(configFolder, "client.yml");

        if(connectorFile.exists() && connectorFile.isDirectory()) {
            try {
                log(Level.ERROR, "No client config file created, please create it.");
                System.exit(-1);
                return;
            } catch(Exception error) {
                log(Level.FATAL, error.getMessage());
                System.exit(-1);
                return;
            }
        }

        log(Level.INFO, "All configurations files checked.");
    }

    /**
     * Load core configurations after checking their existence.
     */
    @SuppressWarnings("unchecked")
    private void loadConfigurations(File configFolder) {
        log(Level.INFO, "Loading client configuration file.");

        try {
            Yaml yaml = new Yaml();
            FileReader reader = new FileReader(new File(configFolder, "client.yml"));
            clientConfiguration = ((Map<String, Object>) yaml.load(reader));
        } catch(FileNotFoundException error) {
            log(Level.FATAL, error.getMessage());
            System.exit(-1);
            return;
        }

        log(Level.INFO, "Client configuration loaded in cache.");
    }

    /**
     * Connect to cache database.
     */
    @SuppressWarnings("unchecked")
    private void databaseConnection() {
        Map<String, String> conf = ((Map<String, String>) clientConfiguration.get("cache"));
        cacheConnector = new SingleCacheConnector(conf.get("hostname"), conf.get("auth"));
        publisher = new Publisher(cacheConnector);
        tasksExecutor = new TasksExecutor();
        alphaManagerClientNetwork = new AlphaManagerClientNetwork(this);

        new Thread(publisher, "PublisherThread").start();
        new Thread(tasksExecutor, "ExecutorThread").start();
    }

    /**
     * Push configurations and packages files into the FTP master server.
     */
    private void checkFTPConnection() {
        log(Level.INFO, "Push configurations and packages into FTP server.");

        log(Level.INFO, "Everything's pushed. Great !");
    }

    /**
     * On system close (normal shutdown)
     */
    private void onDisable() throws FileNotFoundException, UnsupportedEncodingException {
        logger.log(Level.INFO, "Shutting down...");

        // Correctly disconnect redis database
        logger.log(Level.INFO, "Disabling network linkage...");
        alphaManagerClientNetwork.disable();
        logger.log(Level.INFO, "Disabling redis connector...");
        cacheConnector.disable();
    }

    /**
     * Get instance of this class.
     * @return AlphaManagerServerNetwork
     */
    public static AlphaManagerClient getInstance() {
        return instance;
    }

    /**
     * Log message with level.
     * @param level Level of the log
     * @param message Message
     */
    public static void log(Level level, String message) {
        instance.logger.log(level, message + ChatColor.RESET);
    }

    /**
     * Windows os.
     * @return boolean
     */
    public boolean isWindowsOs() {
        return windowsOs;
    }

    /**
     * Get GsonBuilder instance
     * @return Gson
     */
    public Gson getGson() {
        return gson;
    }

    /**
     * Get clients linkage network
     * @return AlphaManagerClientNetwork
     */
    public AlphaManagerClientNetwork getAlphaManagerClientNetwork() {
        return alphaManagerClientNetwork;
    }

    /**
     * Get cache connector database.
     * @return CachingConnector
     */
    public CachingConnector getCacheConnector() {
        return cacheConnector;
    }

    /**
     * Get external ip address.
     * @return String
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Get core configuration
     * @return HashMap
     */
    public Map<String, Object> getClientConfiguration() {
        return clientConfiguration;
    }

    /**
     * Get jedis publisher
     * @return Publisher
     */
    public Publisher getPublisher() {
        return publisher;
    }

    /**
     * Get server hostname
     * @return String
     */
    public String getHostname() {
        return hostname;
    }

    private void displayHeader() {
        String color = ChatColor.YELLOW;

        System.out.println("");
        System.out.println("");
        System.out.println(color + "      ▄▄▄· ▄▄▌   ▄▄▄· ▄ .▄ ▄▄▄· • ▌ ▄ ·.  ▄▄▄·  ▐ ▄  ▄▄▄·  ▄▄ • ▄▄▄ .▄▄▄  ");
        System.out.println(color + "     ▐█ ▀█ ██•  ▐█ ▄███▪▐█▐█ ▀█ ·██ ▐███▪▐█ ▀█ •█▌▐█▐█ ▀█ ▐█ ▀ ▪▀▄.▀·▀▄ █·");
        System.out.println(color + "     ▄█▀▀█ ██▪   ██▀·██▀▐█▄█▀▀█ ▐█ ▌▐▌▐█·▄█▀▀█ ▐█▐▐▌▄█▀▀█ ▄█ ▀█▄▐▀▀▪▄▐▀▀▄ ");
        System.out.println(color + "     ▐█ ▪▐▌▐█▌▐▌▐█▪·•██▌▐▀▐█ ▪▐▌██ ██▌▐█▌▐█ ▪▐▌██▐█▌▐█ ▪▐▌▐█▄▪▐█▐█▄▄▌▐█•█▌");
        System.out.println(color + "      ▀  ▀ .▀▀▀ .▀   ▀▀▀ · ▀  ▀ ▀▀  █▪▀▀▀ ▀  ▀ ▀▀ █▪ ▀  ▀ ·▀▀▀▀  ▀▀▀ .▀  ▀");
        System.out.println(color + "                         ▄▄· ▄▄▌  ▪  ▄▄▄ . ▐ ▄ ▄▄▄▄▄                      ");
        System.out.println(color + "                        ▐█ ▌▪██•  ██ ▀▄.▀·•█▌▐█•██                        ");
        System.out.println(color + "                        ██ ▄▄██▪  ▐█·▐▀▀▪▄▐█▐▐▌ ▐█.▪                      ");
        System.out.println(color + "                        ▐███▌▐█▌▐▌▐█▌▐█▄▄▌██▐█▌ ▐█▌·                      ");
        System.out.println(color + "                  ▀     ·▀▀▀ .▀▀▀ ▀▀▀ ▀▀▀ ▀▀ █▪ ▀▀▀      ▀                ");
        System.out.println(ChatColor.RESET + "");
        System.out.println("                                     " + (isWindowsOs() ? "(Windows Version" : "(Linux version " + System.getProperty("java.vm.vendor", "Debian 8 Jessie x64")) + " || Copyright 2017 MrLizzard)");
        System.out.println("");
        System.out.println("");
    }

}
