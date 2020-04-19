package edge;

import config.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EdgeManager {

    private List<String> edgeHosts;


    public EdgeManager() {
        this.edgeHosts = new ArrayList<String>(Config.EDGE_HOSTS);

        Collections.shuffle(this.edgeHosts);
    }

    public Socket getEdge() {
        int i = 0;
        // Continue to attempt to connect to random Edges until socket successfully established
        while (true) {
            String host = this.edgeHosts.get(i);
            InetSocketAddress edgeAddress = new InetSocketAddress(host, Config.EDGE_PORT);
            try {
                Socket edge = new Socket(edgeAddress.getAddress(), edgeAddress.getPort());
                return edge;
            } catch (IOException e) {
                // If fail to create this socket, make a new choice and attempt to connect to a different edge
            }

            // Increment index to select the next eligible Edge (with wrap-around retry)
            i = (i + 1) % this.edgeHosts.size();
        }
    }

}
