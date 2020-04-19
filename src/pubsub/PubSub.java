package pubsub;

import config.Config;
import irc.IRCMessage;
import util.SocketUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PubSub {

    public static String LOG_PREFIX = "[PubSub]: ";

    private Socket roomSocket;

    private ServerSocket pubSub;

    private Set<Socket> edges;

    private boolean isLoggingResults;

    public PubSub() throws IOException {
        this(true);
    }

    public PubSub(boolean logResults) throws IOException {
        this.edges = ConcurrentHashMap.newKeySet();
        this.isLoggingResults = logResults;

        this.connectToRoom();

        this.pubSub = new ServerSocket(Config.PUBSUB_PORT);
    }

    public void run() {
        log("PubSub launched on port: " + this.pubSub.getLocalPort());
        try {
            new Thread(() -> this.receive()).start();

            while (true) {
                Socket edge = this.pubSub.accept();
                this.subscribe(edge);

                log("Connected to Edge: " + edge.getLocalAddress());
                new Thread(() -> this.edgeHandler(edge)).start();
            }
        } catch (Exception e) {
            // If Room disconnects from PubSub, simply stop the PubSub and proceed to disconnect all sockets
        } finally {
            this.closeAllConnections();
        }
    }

    private void receive() {
        try {
            while (true) {
                BufferedReader messageReader = new BufferedReader(new InputStreamReader(this.roomSocket.getInputStream()));
                IRCMessage message = new IRCMessage(messageReader.readLine());

                log("Received from Room: " + message);
                log("Publishing to Edges: " + message);

                this.publish(message);
            }
        } catch (Exception e) {
            // If Room disconnects from PubSub, simply stop the PubSub and proceed to disconnect all sockets
        } finally {
            this.closeAllConnections();
        }
    }

    public void publish(IRCMessage message) {
        for (Socket edge : this.edges) {
            try {
                message.send(edge.getOutputStream());
            } catch (IOException e) {
                // Best effort write to edge, if fail simply omit this message
            }
        }
    }

    public void subscribe(Socket edge) {
        this.edges.add(edge);
    }

    private void edgeHandler(Socket edge) {
        try {
            while (edge.isConnected()) {
                // Receive a message from the edge, and plumb the edge to the upper-most room
                BufferedReader messageReader = new BufferedReader(new InputStreamReader(edge.getInputStream()));

                IRCMessage message = new IRCMessage(messageReader.readLine());
                message.send(this.roomSocket.getOutputStream());
            }
        } catch (Exception e) {
            // If Edge abruptly disconnects from PubSub, simply proceed to disconnect the Edge
        } finally {
            SocketUtil.bestEffortClose(edge);
        }
    }

    private void connectToRoom() {
        boolean connectLocal = false;

        while (this.roomSocket == null) {
            String host = connectLocal ? Config.LOCALHOST : Config.ROOM_HOST;
            InetSocketAddress roomAddress = new InetSocketAddress(host, Config.ROOM_PORT);
            try {
                this.roomSocket = new Socket(roomAddress.getAddress(), roomAddress.getPort());
            } catch (IOException e) {
                // If fail to connect to the room simply alternate connection attempts between local and remote host
                connectLocal = !connectLocal;
            }
        }
    }

    private void closeAllConnections() {
        SocketUtil.bestEffortClose(this.pubSub);

        for (Socket edge : this.edges) {
            SocketUtil.bestEffortClose(edge);
        }
    }

    private void log(String output) {
        if (this.isLoggingResults) {
            System.out.println(LOG_PREFIX + output);
        }
    }
}
