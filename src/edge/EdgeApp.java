package edge;

import config.CLIConstants;

public class EdgeApp {

    public static void main(String[] args) throws Exception {
        boolean logResults = false;
        if (args.length >= 1) {
            if (CLIConstants.LOG_RESULTS.equals(args[0])) {
                logResults = true;
            }
        }

        Edge edge = new Edge(logResults);
        edge.run();
    }
}
