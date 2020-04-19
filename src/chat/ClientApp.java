package chat;

public class ClientApp {

    public static void main(String[] args) {
        SendThread.getInstance().start();
    }
}