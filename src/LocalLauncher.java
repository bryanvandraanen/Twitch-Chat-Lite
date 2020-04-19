import chat.ClientApp;
import edge.EdgeApp;
import pubsub.PubSubApp;
import room.RoomApp;

public class LocalLauncher {

    public static void main(String[] args) {
        new Thread(() -> runRoomApplication(args)).start();
        new Thread(() -> runPubSubApplication(args)).start();
        new Thread(() -> runEdgeApplication(args)).start();

        ClientApp.main(args);
    }

    public static void runRoomApplication(String[] args) {
        try {
            RoomApp.main(args);
        } catch (Exception e) {
            // If exception is thrown, simply end execution
        }
    }

    public static void runPubSubApplication(String[] args) {
        try {
            PubSubApp.main(args);
        } catch (Exception e) {
            // If exception is thrown, simply end execution
        }
    }

    public static void runEdgeApplication(String[] args) {
        try {
            EdgeApp.main(args);
        } catch (Exception e) {
            // If exception is thrown, simply end execution
        }
    }
}
