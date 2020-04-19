package pubsub;

import config.CLIConstants;

public class PubSubApp {

    public static void main(String[] args) throws Exception {
        boolean logResults = false;
        if (args.length >= 1) {
            if (CLIConstants.LOG_RESULTS.equals(args[0])) {
                logResults = true;
            }
        }
        PubSub pubSub = new PubSub(logResults);
        pubSub.run();
    }
}
