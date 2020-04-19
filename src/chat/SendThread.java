package chat;

import edge.EdgeManager;
import irc.IRCConstants;
import irc.IRCMessage;
import room.CommandConstants;
import room.Room;
import util.SocketUtil;

import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * This thread is used to read users' input, parse it and send it to the server
 * in an IRC Message
 */
public class SendThread extends Thread {

    private static SendThread sendThread;

    private String prompt;

    private Socket socket;
    private OutputStream out;

    private String username;
    private String channel;

    private EdgeManager edgeManager;

    private SendThread() throws Exception {
        this.edgeManager = new EdgeManager();

        this.socket = this.edgeManager.getEdge();

        this.out = socket.getOutputStream();
    }

    public static SendThread getInstance() {
        if (sendThread == null) {
            try {
                sendThread = new SendThread();
            } catch (Exception e) {
                // Unable to create send thread because couldn't connect to Edge - simply exit
                e.printStackTrace();
                System.exit(0);
            }
        }

        return sendThread;
    }

    public synchronized String getCurrentPrompt() {
        return this.prompt;
    }

    private synchronized void setCurrentPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void run() {
        try {
            new ReceiveThread(this.socket).start();
            Scanner in = new Scanner(System.in);
            // Prompt user for their username
            prompt("Enter your username: ");
            this.username = in.nextLine();

            while (this.username.equals(Room.ROOM_BROADCAST)) {
                System.out.println("Invalid username");
                prompt("Enter your username: ");
                this.username = in.nextLine();
            }

            while (true) {
                // Prompt user which channel they want to join
                prompt("Join channel: ");
                String channel = in.nextLine();

                if (channel.equals(Room.ROOM_OVERRIDE)) {
                    System.out.println("Invalid channel");
                    continue;
                }

                this.channel = channel;

                IRCMessage joinMessage = new IRCMessage(this.channel, this.username, CommandConstants.JOIN, null);
                joinMessage.send(this.out);

                String content = "";
                String leaveCommand = IRCConstants.COMMAND_PREFIX + CommandConstants.LEAVE;
                while (!content.equals(leaveCommand)) {
                    prompt(this.username + ": ");
                    content = in.nextLine();

                    if (content.equals("")) {
                        continue;
                    }

                    IRCMessage message = new IRCMessage(this.channel, this.username, content);
                    message.send(this.out);
                }
            }
        } catch (Exception e) {
            // If the Edge disconnects from the client, simply stop the client execution and proceed to socket close
        } finally {
            SocketUtil.bestEffortClose(this.socket);
        }
    }

    public void prompt(String prompt) {
        this.setCurrentPrompt(prompt);
        System.out.print(this.getCurrentPrompt());
    }
}
