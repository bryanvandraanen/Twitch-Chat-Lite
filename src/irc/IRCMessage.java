package irc;

import java.io.IOException;
import java.io.OutputStream;

public class IRCMessage {

    // IRCMessage format (for twitch-chat-lite):
    // channel[sender]content
    // channel[sender]/command content

    private final String channel;

    private final String sender;

    private final String command;

    private final String content;

    // Constructor used to parse received IRC message over network
    public IRCMessage(String line) {
        this.channel = parseChannel(line);
        this.sender = parseSender(line);

        line = removeHeader(line);

        this.command = parseCommand(line);
        this.content = parseContent(line);
    }

    public IRCMessage(String channel, String sender, String content) {
        this(channel, sender, null, content);
    }

    // Constructor used to build a new IRC message based on components
    public IRCMessage(String channel, String sender, String command, String content) {
        this.channel = channel;
        this.sender = sender;
        this.command = command;

        if (content == null) {
            this.content = "";
        } else {
            this.content = content;
        }
    }

    public String getChannel() {
        return this.channel;
    }

    public String getSender() {
        return this.sender;
    }

    public String getCommand() {
        return this.command;
    }

    public String getContent() {
        return this.content;
    }

    public boolean isCommand() {
        return this.command != null;
    }

    public byte[] getBytes() {
        return this.toString().getBytes();
    }

    public void send(OutputStream output) throws IOException {
        output.write(this.getBytes());
        output.write(System.lineSeparator().getBytes());
        output.flush();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(this.channel);
        builder.append(IRCConstants.CHANNEL_END);
        builder.append(this.sender);
        builder.append(IRCConstants.HEADER_END);

        if (this.isCommand()) {
            builder.append(IRCConstants.COMMAND_PREFIX);
            builder.append(this.getCommand());

            if (this.getContent().equals("")) {
                return builder.toString();
            }

            builder.append(" ");
        }

        builder.append(this.getContent());

        return builder.toString();
    }

    private static String parseChannel(String line) {
        int endChannelIndex = line.indexOf(IRCConstants.CHANNEL_END);

        if (endChannelIndex == -1) {
            throw new IllegalArgumentException("Invalid IRC message - channel malformed");
        }

        return line.substring(0, endChannelIndex);
    }

    private static String parseSender(String line) {
        int endChannelIndex = line.indexOf(IRCConstants.CHANNEL_END);
        int endHeaderIndex = line.indexOf(IRCConstants.HEADER_END);

        if (endChannelIndex == -1 || endHeaderIndex == -1) {
            throw new IllegalArgumentException("Invalid IRC message - sender malformed");
        }

        return line.substring(endChannelIndex + 1, endHeaderIndex);
    }

    private static String removeHeader(String line) {
        int endHeaderIndex = line.indexOf(IRCConstants.HEADER_END);

        if (endHeaderIndex == -1) {
            throw new IllegalArgumentException("Invalid IRC message - header malformed");
        }

        return line.substring(endHeaderIndex + 1);
    }

    private static String parseCommand(String line) {
        if (!isCommand(line)) {
            return null;
        }

        int commandEndIndex = line.indexOf(" ");

        if (commandEndIndex == -1) {
            commandEndIndex = line.length();
        }

        return line.substring(1, commandEndIndex);
    }

    private static String parseContent(String line) {
        if (!isCommand(line)) {
            return line;
        } else {
            int commandEndIndex = line.indexOf(" ");

            if (commandEndIndex == -1) {
                return "";
            }

            return line.substring(commandEndIndex + 1);
        }
    }

    private static boolean isCommand(String input) {
        return input.startsWith(IRCConstants.COMMAND_PREFIX);
    }
}
