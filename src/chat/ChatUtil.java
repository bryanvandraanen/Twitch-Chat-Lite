package chat;

import irc.IRCMessage;

import static room.Room.ROOM_BROADCAST;
import static room.Room.ROOM_OVERRIDE;

public class ChatUtil {

    public static String CLEAR_CHAT = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t";

    public static void printIRCMessage(IRCMessage message) {
        StringBuilder builder = new StringBuilder();

        if (!message.getChannel().equals(ROOM_OVERRIDE) && !message.getSender().equals(ROOM_BROADCAST)) {
            builder.append(message.getSender());
            builder.append(": ");
        }

        builder.append(message.getContent());
        builder.append(CLEAR_CHAT);

        System.out.print("\r");
        System.out.println(builder.toString());
        SendThread.getInstance().prompt(SendThread.getInstance().getCurrentPrompt());
    }
}
