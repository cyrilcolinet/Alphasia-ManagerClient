package net.mrlizzard.alphasia.manager.client.core.database.handlers;

public interface PubSubConsumer {

    void consume(String channel, String message);

}
