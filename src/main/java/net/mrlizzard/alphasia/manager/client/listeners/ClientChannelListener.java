package net.mrlizzard.alphasia.manager.client.listeners;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;
import net.mrlizzard.alphasia.manager.client.core.SharingManager;
import net.mrlizzard.alphasia.manager.client.core.database.handlers.PubSubConsumer;
import org.apache.log4j.Level;

public class ClientChannelListener implements PubSubConsumer {

    @Override
    public void consume(String channel, String message) {
        if(message.equalsIgnoreCase("update")) {
            AlphaManagerClient.log(Level.INFO, "Server asking forcing package update. Processing...");
            SharingManager.go(false);
        }
    }

}
