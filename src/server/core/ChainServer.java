package server.core;

import common.exception.StorageException;
import common.util.AppConfig;
import server.storage.StoragePaths;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class ChainServer {

    private static final String CONFIG_KEY_PORT = "server.port";

    private static final int DEFAULT_PORT = 5000;

    private final int port;

    private final AtomicInteger clientCounter = new AtomicInteger(0);

    private volatile boolean isRunning;

    private ServerSocket serverSocket;

    public ChainServer() {
        this(AppConfig.getInstance().getInt(CONFIG_KEY_PORT, DEFAULT_PORT));
    }

    public ChainServer(int port) {
        this.port = port;
    }

    public void start() throws IOException, StorageException {
        StoragePaths.createDirectoriesIfMissing();
        DataSeeder.seedIfEmpty();

        ServerContext.getInstance().initializeServices();

        serverSocket = new ServerSocket(port);
        isRunning = true;
        System.out.println("[Server] listening on port " + port + ". Press Ctrl+C to stop.");

        try {
            acceptClientsUntilStopped();
        } finally {
            closeServerSocket();
            System.out.println("[Server] stopped.");
        }
    }

    private void acceptClientsUntilStopped() throws IOException {
        while (isRunning) {
            try {
                
                Socket clientSocket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler,
                        "client-handler-" + clientCounter.incrementAndGet());
                clientThread.start();

            } catch (IOException acceptFailure) {
                
                if (isRunning) {
                    throw acceptFailure;
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        closeServerSocket();

        ServerContext.getInstance().shutdownBusinessExecutor();
    }

    private void closeServerSocket() {
        if (serverSocket == null || serverSocket.isClosed()) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ignoredFailure) {
            
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] commandLineArguments) {
        ChainServer server = commandLineArguments.length > 0
                ? new ChainServer(Integer.parseInt(commandLineArguments[0]))
                : new ChainServer();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdown-hook"));

        try {
            server.start();
        } catch (IOException networkFailure) {
            System.err.println("[Server] could not open port " + server.getPort()
                    + ": " + networkFailure.getMessage());
        } catch (StorageException storageFailure) {
            System.err.println("[Server] could not prepare the data files: "
                    + storageFailure.getMessage());
        }
    }
}
