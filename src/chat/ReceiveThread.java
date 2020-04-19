package chat;

import irc.IRCMessage;
import util.SocketUtil;

import java.io.*;
import java.net.*;

import static chat.ChatUtil.printIRCMessage;

/**
 * This thread is used to receive server response and print in to the console
 */
public class ReceiveThread extends Thread {
    private Socket socket;
    private BufferedReader buffer;

    public ReceiveThread(Socket socket) {
        try {
            this.socket = socket;
            this.buffer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (Exception e) {
            SocketUtil.bestEffortClose(socket);
        }
    }

    public void run() {
        try {
            while (true) {
                IRCMessage message = new IRCMessage(this.buffer.readLine());

                printIRCMessage(message);
            }
        } catch (Exception e) {
            // If the Edge disconnects from the client, simply stop the client execution and proceed to socket close
        } finally {
            SocketUtil.bestEffortClose(this.socket);
        }
    }
}
