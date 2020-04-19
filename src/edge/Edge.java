package edge;

import config.Config;
import irc.IRCMessage;
import pubsub.PubSubManager;
import room.CommandConstants;
import room.Room;
import util.SocketUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Edge {

    public static String LOG_PREFIX = "[Edge]: ";

    private Socket pubSubSocket;

    private ServerSocket edge;

    private ConcurrentHashMap<String, Socket> clients;

    private ConcurrentHashMap<String, String> memberships;

    private PubSubManager pubSubManager;

    private boolean isLoggingResults;

    public Edge() throws Exception {
        this(true);
    }

    public Edge(boolean logResults) throws Exception {
        this.clients = new ConcurrentHashMap<String, Socket>();
        this.memberships = new ConcurrentHashMap<String, String>();
        this.isLoggingResults = logResults;

        this.pubSubManager = new PubSubManager();

        this.pubSubSocket = this.pubSubManager.getPubSub();

        this.edge = new ServerSocket(Config.EDGE_PORT);
    }

    private boolean hasClient(String username) {
        return this.clients.containsKey(username);
    }

    private void addClient(String username, String channel, Socket client) {
        this.clients.put(username, client);
        this.addMembership(username, channel);
    }

    private void addMembership(String username, String channel) {
        this.memberships.put(username, channel);
    }

    private void removeMembership(String username) {
        this.memberships.remove(username);
    }

    public void run() {
        log("Edge launched on port: " + this.edge.getLocalPort());
        try {
            new Thread(() -> this.receive()).start();

            while (true) {
                Socket client = this.edge.accept();

                log("Connected to Client: " + client.getLocalAddress());
                new Thread(() -> this.clientHandler(client)).start();
            }
        } catch (Exception e) {
            // If PubSub disconnects from the Edge, simply stop the Edge and proceed to disconnect all sockets
        } finally {
            this.closeAllConnections();
        }
    }

    private void receive() {
        try {
            while (true) {
                BufferedReader messageReader = new BufferedReader(new InputStreamReader(this.pubSubSocket.getInputStream()));
                IRCMessage message = new IRCMessage(messageReader.readLine());

                log("Received from PubSub: " + message);

                if (Room.ROOM_OVERRIDE.equals(message.getChannel())) {
                    // This is a message sent specifically to the sender in this case (i.e. Room rejected the message/command)
                    String sender = message.getSender();
                    if (this.hasClient(sender)) {
                        Socket client = this.clients.get(sender);

                        log("Individual message - sending to client " + message.getSender() + ": " + message);
                        message.send(client.getOutputStream());
                    }
                } else {
                    log("Broadcasting message to channel: " + message);
                    String channel = message.getChannel();
                    this.sendToChannel(message, channel);
                }
            }
        } catch (Exception e) {
            // If PubSub disconnects from edge, simply stop the edge and proceed to disconnect all sockets
        } finally {
            this.closeAllConnections();
        }
    }

    private void sendToChannel(IRCMessage message, String channel) {
        for (String username : this.clients.keySet()) {
            try {
                if (channel.equals(this.memberships.get(username)) && !username.equals(message.getSender())) {
                    Socket client = this.clients.get(username);

                    log("Sending to " + username + ": " + message);
                    message.send(client.getOutputStream());
                }
            } catch (IOException e) {
                // Attempt to send message to user, if fail simply proceed to the next user
            }
        }
    }

    private void clientHandler(Socket client) {
        String username = null;
        try {
            BufferedReader messageReader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            IRCMessage message = new IRCMessage(messageReader.readLine());

            username = message.getSender();


            log(username + " connected to edge with message: " + message);

            this.addClient(username, message.getChannel(), client);

            // Send the first message received by the client to the PubSub
            message.send(this.pubSubSocket.getOutputStream());

            while (client.isConnected()) {
                message = new IRCMessage(messageReader.readLine());

                log("Received from " + username + ": " + message);

                // Since PubSubs are assumed to be fault-tolerant and replicated, we can keep track of client
                // actions locally on this edge.  Any command we observe from the client joining or leaving
                // a channel allows us to update our local knowledge of the client to dispatch the correct messages
                if (message.isCommand() && CommandConstants.JOIN.equals(message.getCommand())
                        && !this.memberships.containsKey(username)) {
                    this.addMembership(username, message.getChannel());
                }

                if (message.isCommand() && CommandConstants.LEAVE.equals(message.getCommand())) {
                    this.removeMembership(username);
                }

                message.send(this.pubSubSocket.getOutputStream());
            }
        } catch (Exception e) {
            // If client abruptly disconnects, need to remove them from this edge's managed users
            // Send message indicating that client has left the channel to PubSub
            try {
                String channel = this.memberships.get(username);

                if (channel != null) {
                    new IRCMessage(channel, username, CommandConstants.LEAVE, null).send(this.pubSubSocket.getOutputStream());
                }
            } catch (IOException e1) {
                // Best effort attempt to send leaving message to pubsub
            }
            // Proceed to close client socket and remove membership from this Edge
            SocketUtil.bestEffortClose(client);
            this.clients.remove(username);
            this.removeMembership(username);
        } finally {
            // Clients should only have to connect to an edge once, and after that be able to interact with many chatrooms
            // Close client socket if disconnects
            try {
                String channel = this.memberships.get(username);

                if (channel != null) {
                    new IRCMessage(channel, username, CommandConstants.LEAVE, null).send(this.pubSubSocket.getOutputStream());
                }
            } catch (IOException e1) {
                // Best effort attempt to send leaving message to pubsub
            }
            SocketUtil.bestEffortClose(client);
        }
    }

    private void closeAllConnections() {
        SocketUtil.bestEffortClose(this.edge);
        SocketUtil.bestEffortClose(this.pubSubSocket);

        for (Socket client : this.clients.values()) {
            SocketUtil.bestEffortClose(client);
        }
    }

    private void log(String output) {
        if (this.isLoggingResults) {
            System.out.println(LOG_PREFIX + output);
        }
    }
}
