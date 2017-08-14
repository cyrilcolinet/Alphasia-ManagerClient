package net.mrlizzard.alphasia.manager.client.core.database.networking;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;
import net.mrlizzard.alphasia.manager.client.core.database.Publisher;
import net.mrlizzard.alphasia.manager.client.core.database.handlers.PubSubConsumer;
import net.mrlizzard.alphasia.manager.client.core.database.handlers.SubscribingThread;
import net.mrlizzard.alphasia.manager.client.listeners.ClientChannelListener;
import net.mrlizzard.alphasia.manager.client.listeners.LocalClientChannelListener;
import net.mrlizzard.alphasia.manager.client.listeners.OpenServerListener;
import net.mrlizzard.alphasia.manager.client.tasks.ClientHeartbeetTask;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;

public class AlphaManagerClientNetwork {

    protected AlphaManagerClient                            instance;

    private final SubscribingThread thread;

    public AlphaManagerClientNetwork(AlphaManagerClient instance) {
        this.instance = instance;
        AlphaManagerClient.log(Level.INFO, "Initialized clients connector...");

        if (instance.getCacheConnector().getCacheResource().hsetnx("alphamanager:clients", instance.getHostname(), instance.getIpAddress()) == 0) {
            thread = null;

            AlphaManagerClient.log(Level.ERROR, "This client IP address is already registered in clients instances.");
            System.exit(-1);
        } else {
            instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.client", "start " + AlphaManagerClient.getInstance().getHostname()));
            instance.getCacheConnector().getCacheResource().hset("alphamanager:clients", AlphaManagerClient.getInstance().getHostname(), AlphaManagerClient.getInstance().getIpAddress());

            instance.getCacheConnector().subscribe("alphamanager.client.open", new OpenServerListener());
            instance.getCacheConnector().subscribe("alphamanager.client.all", new ClientChannelListener());

            String[] hostContent = StringUtils.split(instance.getHostname().toLowerCase(), ".");
            PubSubConsumer localConsumer = new LocalClientChannelListener();

            if(hostContent.length >= 3 && hostContent[hostContent.length - 1].equals("fr") && hostContent[hostContent.length - 2].equals("alphasia")) {
                instance.getCacheConnector().subscribe("alphamanager.client." + hostContent[0], localConsumer);
            } else {
                instance.getCacheConnector().subscribe("alphamanager.client." + instance.getHostname().toLowerCase(), localConsumer);
            }

            // Init heartbeet
            new ClientHeartbeetTask(this::heartBeet);

            thread = new SubscribingThread(SubscribingThread.Type.SUBSCRIBE, AlphaManagerClient.getInstance().getCacheConnector(), new Subscriber(this), "alphamanager");
            new Thread(thread).start();
        }
    }

    private void heartBeet() {
        instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.client", "heartbeat " + instance.getHostname()));
    }

    public void disable() {
        AlphaManagerClient.log(Level.INFO, "Clearing old cache...");
        instance.getPublisher().publish(new Publisher.PendingMessage("alphamanager.client", "stop " + instance.getHostname()));
        instance.getCacheConnector().getCacheResource().hdel("alphamanager:clients", instance.getHostname());
    }

}
