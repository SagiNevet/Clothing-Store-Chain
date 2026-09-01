package client.gui;

import client.controller.ClientSession;
import client.net.ServerEventListener;
import common.exception.ChainStoreException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.LayoutManager;

public abstract class ServerBackedPanel extends JPanel implements ServerEventListener {

    private static final long serialVersionUID = 1L;

    protected ServerBackedPanel(LayoutManager layoutManager) {
        super(layoutManager);
        ClientSession.getInstance().getConnection().getEventDispatcher().subscribe(this);
    }

    public void detachFromServerEvents() {
        ClientSession.getInstance().getConnection().getEventDispatcher().unsubscribe(this);
    }

    protected void runInBackground(String threadName, ServerWork work) {
        new Thread(() -> {
            try {
                work.perform();
            } catch (ChainStoreException serverFailure) {
                SwingUtilities.invokeLater(() -> showError(serverFailure.getMessage()));
            }
        }, threadName).start();
    }

    protected void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Action failed",
                JOptionPane.ERROR_MESSAGE);
    }

    protected void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Done",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public interface ServerWork {

        void perform() throws ChainStoreException;
    }
}
