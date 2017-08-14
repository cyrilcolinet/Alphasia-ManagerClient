package net.mrlizzard.alphasia.manager.client.listeners;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;
import net.mrlizzard.alphasia.manager.client.core.database.handlers.PubSubConsumer;
import org.apache.log4j.Level;

public class LocalClientChannelListener implements PubSubConsumer {

    @Override
    public void consume(String channel, String message) {
        if(message.startsWith("command")) {
            message = message.replace("command ", "");

            if(message.equalsIgnoreCase("close")) {
                AlphaManagerClient.log(Level.INFO, "Server asking shutdown.");
                System.exit(-1);
            }
        }
    }
}
