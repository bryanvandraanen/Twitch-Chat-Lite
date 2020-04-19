package config;

import java.util.Arrays;
import java.util.List;

public class Config {
    public static String LOCALHOST = "localhost";

    public static String ROOM_HOST = LOCALHOST;
    public static int ROOM_PORT = 65535;

    public static List<String> PUBSUB_HOSTS = Arrays.asList(LOCALHOST, "attu6.cs.washington.edu");
    public static int PUBSUB_PORT = 65534;

    public static List<String> EDGE_HOSTS = Arrays.asList(LOCALHOST, "attu7.cs.washington.edu", "attu8.cs.washington.edu");
    public static int EDGE_PORT = 65533;
}
