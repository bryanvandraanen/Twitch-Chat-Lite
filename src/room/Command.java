package room;

import clue.Clue;

public class Command {

    public static CommandResponseCode join(Room room, String channel, String instigator) {
        boolean newChatRoom = !room.hasChatRoom(channel);
        Clue chatRoom = room.getChatRoom(channel);

        // If this is the first time this chat room is created (i.e. first person to join, mod the name of the channel ("streamer"))
        if (newChatRoom) {
            chatRoom.mod(channel);
        }

        if (chatRoom.isMember(instigator)) {
            return CommandResponseCode.INVALID_MEMBER;
        }

        chatRoom.join(instigator);

        return CommandResponseCode.SUCCESS;
    }

    public static CommandResponseCode leave(Room room, String channel, String instigator) {
        if (!room.hasChatRoom(channel)) {
            return CommandResponseCode.CHANNEL_NOT_EXIST;
        }

        Clue chatRoom = room.getChatRoom(channel);

        // Leave this chat room if previously a member (
        if (chatRoom.isMember(instigator)) {
            chatRoom.leave(instigator);
            return CommandResponseCode.SUCCESS;
        }

        return CommandResponseCode.INVALID_MEMBER;
    }

    public static CommandResponseCode ban(Room room, String channel, String member, String instigator) {
        if (!room.hasChatRoom(channel)) {
            return CommandResponseCode.CHANNEL_NOT_EXIST;
        }

        Clue chatRoom = room.getChatRoom(channel);

        // Only allow this moderator command if instigator has mod privileges
        if (chatRoom.isMod(instigator)) {
            chatRoom.ban(member);
            return CommandResponseCode.SUCCESS;
        }

        return CommandResponseCode.INVALID_PRIVILEGES;
    }

    public static CommandResponseCode unban(Room room, String channel, String member, String instigator) {
        if (!room.hasChatRoom(channel)) {
            return CommandResponseCode.CHANNEL_NOT_EXIST;
        }

        Clue chatRoom = room.getChatRoom(channel);

        // Only allow this moderator command if instigator has mod privileges
        if (chatRoom.isMod(instigator) && chatRoom.isBanned(member)) {
            chatRoom.unban(member);
            return CommandResponseCode.SUCCESS;
        }

        return !chatRoom.isMod(instigator) ? CommandResponseCode.INVALID_PRIVILEGES : CommandResponseCode.INVALID_MEMBER;
    }

    public static CommandResponseCode mod(Room room, String channel, String member, String instigator) {
        if (!room.hasChatRoom(channel)) {
            return CommandResponseCode.CHANNEL_NOT_EXIST;
        }

        Clue chatRoom = room.getChatRoom(channel);

        // Only allow this moderator command if instigator has mod privileges
        if (chatRoom.isMod(instigator)) {
            chatRoom.mod(member);
            return CommandResponseCode.SUCCESS;
        }

        return CommandResponseCode.INVALID_PRIVILEGES;
    }

    public static CommandResponseCode unmod(Room room, String channel, String member, String instigator) {
        if (!room.hasChatRoom(channel)) {
            return CommandResponseCode.CHANNEL_NOT_EXIST;
        }

        Clue chatRoom = room.getChatRoom(channel);

        // Only allow this moderator command if instigator has mod privileges
        if (chatRoom.isMod(instigator)) {
            chatRoom.unmod(member);
            return CommandResponseCode.SUCCESS;
        }

        return !chatRoom.isMod(instigator) ? CommandResponseCode.INVALID_PRIVILEGES : CommandResponseCode.INVALID_MEMBER;
    }
}
