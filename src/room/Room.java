package room;

import clue.Clue;
import config.Config;
import irc.IRCMessage;
import util.SocketUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Room {

    public static String LOG_PREFIX = "[Room]: ";

    public static final String ROOM_OVERRIDE = "$ROOM";

    public static final String ROOM_BROADCAST = "$BROADCAST";

    private ConcurrentHashMap<String, Clue> chatRooms;

    private ServerSocket room;

    private Set<Socket> pubSubs;

    private boolean isLoggingResults;

    public Room() throws IOException {
        this(true);
    }

    public Room(boolean logResults) throws IOException {
        this.chatRooms = new ConcurrentHashMap<String, Clue>();
        this.pubSubs = new HashSet<Socket>();
        this.isLoggingResults = logResults;

        this.room = new ServerSocket(Config.ROOM_PORT);
    }

    public boolean hasChatRoom(String channel) {
        return this.chatRooms.containsKey(channel);
    }

    public Clue getChatRoom(String channel) {
        if (!this.hasChatRoom(channel)) {
            this.createChatRoom(channel);
        }

        return this.chatRooms.get(channel);
    }

    public void run() {
        log("Room launched on port: " + this.room.getLocalPort());
        try {
            while (true) {
                Socket pubSub = this.room.accept();
                this.pubSubs.add(pubSub);

                log("Connected to PubSub: " + pubSub.getLocalAddress());
                new Thread(() -> this.pubSubHandler(pubSub)).start();
            }
        } catch (Exception e) {
            // If Room abruptly disconnects, proceed to disconnect all connections
        } finally {
            this.closeAllConnections();
        }
    }

    private void createChatRoom(String channel) {
        if (this.hasChatRoom(channel)) {
            throw new IllegalArgumentException("Channel already exists: " + channel);
        }

        this.chatRooms.put(channel, new Clue());
    }

    public void pubSubHandler(Socket pubSub) {
        try {
            while (pubSub.isConnected()) {
                BufferedReader messageReader = new BufferedReader(new InputStreamReader(pubSub.getInputStream()));

                IRCMessage message = new IRCMessage(messageReader.readLine());

                if (message.isCommand()) {
                    String command = message.getCommand();
					
                    log("Received command from PubSub: " + message);

                    if (command.equals(CommandConstants.JOIN)) {
                        this.join(message);
                    } else if (command.equals(CommandConstants.LEAVE)) {
                        this.leave(message);
                    } else if (command.equals(CommandConstants.BAN)) {
                        this.ban(message);
                    } else if (command.equals(CommandConstants.UNBAN)) {
                        this.unban(message);
                    } else if (command.equals(CommandConstants.MOD)) {
                        this.mod(message);
                    } else if (command.equals(CommandConstants.UNMOD)) {
                        this.unmod(message);
                    } else {
                        this.invalidCommand(message);
                        // ILLEGAL COMMAND GIVEN - RETURN RESPONSE ACCORDINGLY (to only the sender's room?)
                    }
                } else {
                    Clue chatRoom = this.getChatRoom(message.getChannel());

                    log("Received message from PubSub: " + message);

                    if (!chatRoom.isBanned(message.getSender())) {
                        log("Broadcasting to PubSubs: " + message);
                        sendToPubSubs(message);
                    } else {
                        IRCMessage banResponse = new IRCMessage(ROOM_OVERRIDE, message.getSender(),
                                "You are banned and unable to chat in this channel");
                        log(message.getSender() + " is banned - broadcasting to PubSubs: " + banResponse);
                        sendToPubSubs(banResponse);
                    }
                }
            }
        } catch (Exception e) {
            // If PubSub abruptly disconnects, proceed to disconnect the PubSub from the room
        } finally {
            this.pubSubs.remove(pubSub);
            SocketUtil.bestEffortClose(pubSub);
        }
    }

    private void join(IRCMessage message) {
        CommandResponseCode code = Command.join(this, message.getChannel(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getSender() +
                    " joined the channel");

            log(message.getSender() + " joined " + message.getChannel() + " successfully");
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(ROOM_OVERRIDE, message.getSender(), message.getSender() +
                    " is already in a channel - type /leave to leave the channel");

            log(message.getSender() + " is already in a channel");
            log("Broadcasting to PubSubs: " + response);
            sendToPubSubs(response);
        }
    }

    private void leave(IRCMessage message) {
        CommandResponseCode code = Command.leave(this, message.getChannel(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getSender() + " left the channel");

            log(message.getSender() + " left " + message.getChannel() + " successfully");
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(message.getChannel(), message.getSender(), "failed to leave the channel");

            log(message.getSender() + " failed to leave channel " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        }
    }

    private void ban(IRCMessage message) {
        CommandResponseCode code = Command.ban(this, message.getChannel(), message.getContent(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getContent() + " is banned");

            log(message.getSender() + " is now banned in " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(ROOM_OVERRIDE, message.getSender(), "You are not a moderator in this channel");

            log(message.getSender() + " is not allowed to ban members in " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        }
    }

    private void unban(IRCMessage message) {
        CommandResponseCode code = Command.unban(this, message.getChannel(), message.getContent(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getContent() + " is unbanned");

            log(message.getSender() + " unbanned " + message.getContent() + " in channel " + message.getChannel() + " successfully");
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(ROOM_OVERRIDE, message.getSender(), "You are not a moderator in this channel");

            log(message.getSender() + " is not allowed to unban members in " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(new IRCMessage(ROOM_OVERRIDE, message.getSender(), "You are not a moderator in this channel"));
        }
    }

    private void mod(IRCMessage message) {
        CommandResponseCode code = Command.mod(this, message.getChannel(), message.getContent(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getContent() + " is now a moderator");

            log(message.getSender() + " mod'd " + message.getContent() + " in channel " + message.getChannel() + " successfully");
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(ROOM_OVERRIDE, message.getSender(), "You are not a moderator in this channel");

            log(message.getSender() + " is not allowed to mod members in " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        }
    }

    private void unmod(IRCMessage message) {
        CommandResponseCode code = Command.unmod(this, message.getChannel(), message.getContent(), message.getSender());

        if (code == CommandResponseCode.SUCCESS) {
            IRCMessage response = new IRCMessage(message.getChannel(), ROOM_BROADCAST, message.getContent() + " is no longer a moderator");

            log(message.getSender() + " unmod'd " + message.getContent() + " in channel " + message.getChannel() + " successfully");
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        } else {
            IRCMessage response = new IRCMessage(ROOM_OVERRIDE, message.getSender(), "You are not a moderator in this channel");

            log(message.getSender() + " is not allowed to unmod members in " + message.getChannel());
            log("Broadcasting to PubSubs: " + response);

            sendToPubSubs(response);
        }
    }

    private void invalidCommand(IRCMessage message) {
        sendToPubSubs(new IRCMessage(ROOM_OVERRIDE, message.getSender(), "Invalid command: " + message.getCommand()));
    }

    private void sendToPubSubs(IRCMessage message) {
        for (Socket pubSub : this.pubSubs) {
            try {
                message.send(pubSub.getOutputStream());
            } catch (Exception e) {
                // Best effort write to pubsubs, if fail simply omit this message
            }
        }
    }

    private void closeAllConnections() {
        SocketUtil.bestEffortClose(this.room);

        for (Socket pubSub : this.pubSubs) {
            SocketUtil.bestEffortClose(pubSub);
        }
    }

    private void log(String output) {
        if (this.isLoggingResults) {
            System.out.println(LOG_PREFIX + output);
        }
    }
}
