package server.core;

import common.exception.StorageException;
import common.util.AppConfig;
import server.storage.StoragePaths;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The server of the chain: listens for clients and gives each one a thread of
 * its own.
 * <p>
 * <b>The accept loop</b> is the heart of the class. {@code accept()} blocks
 * until a client connects, and the moment it returns a socket that socket is
 * handed to a brand new thread and the loop goes straight back to waiting. The
 * loop itself therefore does almost nothing, which is why the number of clients
 * is not limited in any way - exactly as the requirement demands.
 * </p>
 * <p>
 * <b>Stopping the server</b> is done with the {@code volatile} flag
 * {@link #isRunning} and never with {@code Thread.stop()}. There is one
 * subtlety worth knowing: a thread blocked inside {@code accept()} does not
 * notice a flag changing. Closing the {@link ServerSocket} is what wakes it up,
 * by making {@code accept()} throw; the loop then reads the flag, sees that the
 * stop was intentional, and ends quietly.
 * </p>
 */
public class ChainServer {

    /** Configuration key holding the port the server listens on. */
    private static final String CONFIG_KEY_PORT = "server.port";

    /** The port used when the configuration file is missing. */
    private static final int DEFAULT_PORT = 5000;

    /** The port this server listens on. */
    private final int port;

    /** Counts the clients that have connected, used to name the threads. */
    private final AtomicInteger clientCounter = new AtomicInteger(0);

    /**
     * Whether the accept loop should keep running. Declared {@code volatile} so
     * that a stop requested by another thread is seen immediately.
     */
    private volatile boolean isRunning;

    /** The listening socket, kept as a field so that {@link #stop()} can close it. */
    private ServerSocket serverSocket;

    /**
     * Creates a server that listens on the port taken from the configuration
     * file.
     */
    public ChainServer() {
        this(AppConfig.getInstance().getInt(CONFIG_KEY_PORT, DEFAULT_PORT));
    }

    /**
     * Creates a server that listens on a given port. Used by the tests, which
     * need a port of their own so they never collide with a running server.
     *
     * @param port the port to listen on
     */
    public ChainServer(int port) {
        this.port = port;
    }

    /**
     * Prepares the data files and then runs the accept loop until the server is
     * stopped.
     * <p>
     * This method blocks for as long as the server is alive, so it is called
     * either from {@code main} or from a thread of its own.
     * </p>
     *
     * @throws IOException      if the listening socket cannot be opened
     * @throws StorageException if the demonstration data cannot be created
     */
    public void start() throws IOException, StorageException {
        StoragePaths.createDirectoriesIfMissing();
        DataSeeder.seedIfEmpty();
        // The order matters: the services load the data files into memory, so
        // they may only be built after the seeder has written those files.
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

    /**
     * Accepts one client after another and starts a thread for each of them.
     *
     * @throws IOException if the listening socket fails while the server is
     *                     still supposed to be running
     */
    private void acceptClientsUntilStopped() throws IOException {
        while (isRunning) {
            try {
                // Blocks here until somebody connects. This is the only place in
                // the server that waits for a new client.
                Socket clientSocket = serverSocket.accept();

                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler,
                        "client-handler-" + clientCounter.incrementAndGet());
                clientThread.start();

            } catch (IOException acceptFailure) {
                // Closing the server socket makes accept() throw. When the stop was
                // intentional that is not an error, so the loop simply ends.
                if (isRunning) {
                    throw acceptFailure;
                }
            }
        }
    }

    /**
     * Asks the server to stop accepting clients.
     * <p>
     * The flag is lowered first and the listening socket is closed second. The
     * order matters: closing the socket is what wakes the thread that is
     * blocked inside {@code accept()}, and by then the flag already tells it
     * that the failure was intentional.
     * </p>
     */
    public void stop() {
        isRunning = false;
        closeServerSocket();
        // The pool is stopped after the socket, so operations that were already
        // in progress are given their chance to finish rather than being cut off.
        ServerContext.getInstance().shutdownBusinessExecutor();
    }

    /**
     * Closes the listening socket if it is still open.
     */
    private void closeServerSocket() {
        if (serverSocket == null || serverSocket.isClosed()) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ignoredFailure) {
            // The server is shutting down; there is nothing useful left to do.
        }
    }

    /**
     * Indicates whether the accept loop is running.
     *
     * @return {@code true} while the server is listening
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Returns the port this server listens on.
     *
     * @return the listening port
     */
    public int getPort() {
        return port;
    }

    /**
     * Starts the server from the command line.
     *
     * @param commandLineArguments an optional single argument holding the port
     *                             number, which overrides the configuration file
     */
    public static void main(String[] commandLineArguments) {
        ChainServer server = commandLineArguments.length > 0
                ? new ChainServer(Integer.parseInt(commandLineArguments[0]))
                : new ChainServer();

        // Closing the console window or pressing Ctrl+C runs this hook, which
        // lowers the flag and closes the listening socket in an orderly way.
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
