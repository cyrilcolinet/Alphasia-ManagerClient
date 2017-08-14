package net.mrlizzard.alphasia.manager.client.core.database.networking;

import redis.clients.jedis.JedisPubSub;

public class Subscriber extends JedisPubSub {

    private final AlphaManagerClientNetwork networkServer;

    public Subscriber(AlphaManagerClientNetwork networkServer) {
        this.networkServer = networkServer;
    }

    @Override
    public void onMessage(String channel, String message) {
        String[] content = message.split(" ");
        System.out.println(message);
    }

}
