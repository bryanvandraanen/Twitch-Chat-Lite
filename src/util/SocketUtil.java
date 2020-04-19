package util;

import java.net.ServerSocket;
import java.net.Socket;

public class SocketUtil {

    public static void bestEffortClose(ServerSocket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            // Best effort closing of the socket
        }
    }

    public static void bestEffortClose(Socket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            // Best effort closing of the socket
        }
    }
}
