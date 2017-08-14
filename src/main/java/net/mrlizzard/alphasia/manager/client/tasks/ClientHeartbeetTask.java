package net.mrlizzard.alphasia.manager.client.tasks;

import net.mrlizzard.alphasia.manager.client.AlphaManagerClient;
import net.mrlizzard.alphasia.manager.client.utils.CustomThreadedTask;
import org.apache.log4j.Level;

public class ClientHeartbeetTask extends CustomThreadedTask {

    private Runnable runnable;

    public ClientHeartbeetTask(Runnable runnable) {
        super(0, 5000);

        this.runnable = runnable;
        AlphaManagerClient.log(Level.INFO, "Client heartbeat task started !");
    }

    @Override
    public void run() {
        runnable.run();
    }

}
