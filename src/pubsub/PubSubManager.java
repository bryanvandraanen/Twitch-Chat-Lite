package pubsub;

import config.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PubSubManager {

    private List<String> pubSubHosts;

    public PubSubManager() {
        this.pubSubHosts = new ArrayList<String>(Config.PUBSUB_HOSTS);

        Collections.shuffle(this.pubSubHosts);
    }

    public Socket getPubSub() {
        int i = 0;
        // Continue to attempt to connect to random PubSubs until socket successfully established
        while (true) {
            String host = this.pubSubHosts.get(i);
            InetSocketAddress pubSubAddress = new InetSocketAddress(host, Config.PUBSUB_PORT);
            try {
                Socket pubSub = new Socket(pubSubAddress.getAddress(), pubSubAddress.getPort());
                return pubSub;
            } catch (IOException e) {
                // If fail to create this socket, make a new choice and attempt to connect to a different pubSub
            }

            // Increment index to select the next eligible PubSub (with wrap-around retry)
            i = (i + 1) % this.pubSubHosts.size();
        }
    }
}
