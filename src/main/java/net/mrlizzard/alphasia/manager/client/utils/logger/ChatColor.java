package net.mrlizzard.alphasia.manager.client.utils.logger;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;

public class ChatColor {

    // ChatColor fields
    public static final String      RESET        = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[0m" : "";
    public static final String      BLACK        = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[30m" : "";
    public static final String      RED          = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[31m" : "";
    public static final String      GREEN        = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[32m" : "";
    public static final String      YELLOW       = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[33m" : "";
    public static final String      BLUE         = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[34m" : "";
    public static final String      PURPLE       = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[35m" : "";
    public static final String      CYAN         = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[36m" : "";
    public static final String      WHITE        = !AlphaManagerClient.getInstance().isWindowsOs() ? "\u001B[37m" : "";

}
