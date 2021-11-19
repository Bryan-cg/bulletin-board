package server;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Main {
    private static int PORT = 1099;

    public static void main(String[] args) {
        Main main = new Main();
        main.startServer();
    }

    private void startServer() {
        try {
            Registry registry = LocateRegistry.createRegistry(PORT);
            registry.rebind("ChatService", new ChatImpl());

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.printf("Server bulletin board running on port %d%n", PORT);
    }
}
