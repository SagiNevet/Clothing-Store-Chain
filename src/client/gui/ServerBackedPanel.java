package client.gui;

import client.controller.ClientSession;
import client.net.ServerEventListener;
import common.exception.ChainStoreException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.LayoutManager;

/**
 * The shared base of every screen that talks to the server.
 * <p>
 * Two things are needed by all of them, and both are easy to get wrong, so they
 * are written once here:
 * </p>
 * <ol>
 *   <li><b>Never block the Swing thread.</b> {@link #runInBackground} performs
 *       the network call on a thread of its own and reports a failure in a
 *       dialog, on the Swing thread. Without this, every request would freeze
 *       the window until the server answered.</li>
 *   <li><b>Always unsubscribe.</b> A panel registers itself as a
 *       {@link ServerEventListener} while it is open. A panel that forgot to
 *       unregister would be kept alive by the dispatcher forever and would keep
 *       receiving events for a window that no longer exists.</li>
 * </ol>
 */
public abstract class ServerBackedPanel extends JPanel implements ServerEventListener {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /**
     * Builds the panel and registers it to receive events pushed by the server.
     *
     * @param layoutManager the layout this panel arranges its components with
     */
    protected ServerBackedPanel(LayoutManager layoutManager) {
        super(layoutManager);
        ClientSession.getInstance().getConnection().getEventDispatcher().subscribe(this);
    }

    /**
     * Stops this panel from receiving events, when its window is closed.
     */
    public void detachFromServerEvents() {
        ClientSession.getInstance().getConnection().getEventDispatcher().unsubscribe(this);
    }

    /**
     * Runs a piece of work that talks to the server, off the Swing thread.
     * <p>
     * A business failure - refused permission, missing stock, an unknown
     * customer - is caught here and shown to the user in one place, so the
     * screens themselves stay free of error handling.
     * </p>
     *
     * @param threadName a short name for the thread, useful while debugging
     * @param work       the work to perform against the server
     */
    protected void runInBackground(String threadName, ServerWork work) {
        new Thread(() -> {
            try {
                work.perform();
            } catch (ChainStoreException serverFailure) {
                SwingUtilities.invokeLater(() -> showError(serverFailure.getMessage()));
            }
        }, threadName).start();
    }

    /**
     * Shows a failure to the user. Must be called on the Swing thread.
     *
     * @param message the message to display
     */
    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Action failed",
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a confirmation to the user. Must be called on the Swing thread.
     *
     * @param message the message to display
     */
    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * A piece of work that talks to the server and may fail for a business
     * reason.
     * <p>
     * This is a small interface of our own rather than {@code Runnable},
     * because {@code Runnable} is not allowed to throw a checked exception -
     * and every call to the server can throw {@link ChainStoreException}.
     * </p>
     */
    public interface ServerWork {

        /**
         * Performs the work.
         *
         * @throws ChainStoreException if the server refused the request or could
         *                             not be reached
         */
        void perform() throws ChainStoreException;
    }
}
