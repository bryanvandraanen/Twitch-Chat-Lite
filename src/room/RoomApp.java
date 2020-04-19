package room;

import config.CLIConstants;

import java.io.IOException;

public class RoomApp {

    public static void main(String[] args) throws IOException {
        boolean logResults = false;
        if (args.length >= 1) {
            if (CLIConstants.LOG_RESULTS.equals(args[0])) {
                logResults = true;
            }
        }

        Room room = new Room(logResults);
        room.run();
    }
}
