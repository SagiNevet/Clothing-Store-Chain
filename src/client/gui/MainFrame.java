package client.gui;

import client.controller.ClientSession;
import client.controller.LoginController;
import client.net.ServerEventListener;
import common.exception.ChainStoreException;
import common.model.Employee;
import common.model.Role;
import common.protocol.EventType;
import common.protocol.ServerEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The main window of the client, built according to the role and the branch of
 * the employee that logged in.
 * <p>
 * <b>The view really does change per user</b>, which is one of the
 * requirements. The header names the branch, and the tabs are added according
 * to {@link Role}: a cashier or a seller sees inventory, customers and chat,
 * while a shift manager also sees the employees tab and the reports tab. The
 * server refuses those actions anyway, but there is no reason to show a user a
 * button that will only produce a refusal.
 * </p>
 * <p>
 * The window is also a {@link ServerEventListener}: it subscribes to the events
 * pushed by the server so it can react to things that happen elsewhere, such as
 * being disconnected by the server.
 * </p>
 */
public class MainFrame extends JFrame implements ServerEventListener {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The width of the window in pixels. */
    private static final int WINDOW_WIDTH = 900;

    /** The height of the window in pixels. */
    private static final int WINDOW_HEIGHT = 600;

    /** The size of the title in the header. */
    private static final float HEADER_FONT_SIZE = 16f;

    /** The employee this window belongs to. */
    private final transient Employee employee;

    /** The controller used to log out. */
    private final transient LoginController loginController = new LoginController();

    /** The inventory screen, kept so it can be unsubscribed when the window closes. */
    private InventoryPanel inventoryPanel;

    /** The customers screen, kept so it can be unsubscribed when the window closes. */
    private CustomersPanel customersPanel;

    /** The employees screen, built only for a shift manager, may stay null. */
    private EmployeesPanel employeesPanel;

    /** The chat screen, kept so it can be unsubscribed when the window closes. */
    private ChatPanel chatPanel;

    /** The reports screen, built only for a shift manager, may stay null. */
    private ReportsPanel reportsPanel;

    /**
     * Builds the main window for one employee.
     *
     * @param employee the employee that has just logged in
     */
    public MainFrame(Employee employee) {
        this.employee = employee;

        setTitle("Clothing Store Chain - " + employee.getBranch().getDisplayName()
                + " - " + employee.getFullName());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        // The window is closed by hand in the listener below, so the session can
        // be released on the server before the program ends.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTabsForRole(), BorderLayout.CENTER);

        ClientSession.getInstance().getConnection().getEventDispatcher().subscribe(this);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent closingEvent) {
                closeApplication();
            }
        });
    }

    /**
     * Builds the strip at the top of the window showing who is logged in.
     *
     * @return the header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel identityLabel = new JLabel(employee.getFullName()
                + "  |  " + employee.getRole().getDisplayName()
                + "  |  " + employee.getBranch().getDisplayName()
                + "  |  employee no. " + employee.getEmployeeNumber());
        identityLabel.setFont(identityLabel.getFont().deriveFont(Font.BOLD, HEADER_FONT_SIZE));

        JButton logoutButton = new JButton("Log out");
        logoutButton.addActionListener(actionEvent -> performLogout());

        headerPanel.add(identityLabel, BorderLayout.WEST);
        headerPanel.add(logoutButton, BorderLayout.EAST);
        return headerPanel;
    }

    /**
     * Builds the tabs this employee is allowed to see.
     * <p>
     * The decision is taken from the permission methods of {@link Role} rather
     * than from a comparison such as {@code role == SHIFT_MANAGER}. If a
     * permission ever changes, it changes in the enum and every screen follows.
     * </p>
     *
     * @return the tabbed pane holding the screens of this role
     */
    private JTabbedPane createTabsForRole() {
        JTabbedPane tabs = new JTabbedPane();

        inventoryPanel = new InventoryPanel();
        customersPanel = new CustomersPanel();

        tabs.addTab("Inventory", inventoryPanel);
        tabs.addTab("Customers", customersPanel);

        chatPanel = new ChatPanel();
        tabs.addTab("Chat", chatPanel);

        if (employee.getRole().canManageEmployees()) {
            employeesPanel = new EmployeesPanel();
            tabs.addTab("Employees", employeesPanel);
        }

        if (employee.getRole().canViewReports()) {
            reportsPanel = new ReportsPanel();
            tabs.addTab("Reports", reportsPanel);
        }

        return tabs;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Called on the Swing thread by the dispatcher, so the window may be
     * changed directly from here.
     * </p>
     */
    @Override
    public void onServerEvent(ServerEvent event) {
        if (event.getEventType() == EventType.FORCED_LOGOUT) {
            JOptionPane.showMessageDialog(this,
                    "The server closed your session.",
                    "Session closed", JOptionPane.WARNING_MESSAGE);
            returnToLoginScreen();
        }
    }

    /**
     * Logs out and returns to the login screen, without closing the program.
     */
    private void performLogout() {
        // The network call runs on a thread of its own. Doing it on the Swing
        // thread would freeze the window until the server answered.
        new Thread(() -> {
            try {
                loginController.logout();
            } catch (ChainStoreException logoutFailure) {
                System.err.println("[Client] logout failed: " + logoutFailure.getMessage());
            }
            SwingUtilities.invokeLater(this::returnToLoginScreen);
        }, "logout-worker").start();
    }

    /**
     * Closes this window and opens a fresh login screen.
     */
    private void returnToLoginScreen() {
        detachAllPanelsFromServerEvents();
        ClientSession.getInstance().clearCurrentEmployee();
        dispose();
        new LoginFrame().setVisible(true);
    }

    /**
     * Unsubscribes this window and every panel inside it from the pushed events.
     * <p>
     * This matters more than it looks. The dispatcher holds a reference to every
     * subscriber, so a panel that never unsubscribed would stay alive for the
     * whole run of the program and would keep receiving events for a window that
     * has already been closed - a memory leak and a source of strange errors.
     * </p>
     */
    private void detachAllPanelsFromServerEvents() {
        ClientSession.getInstance().getConnection().getEventDispatcher().unsubscribe(this);
        if (inventoryPanel != null) {
            inventoryPanel.detachFromServerEvents();
        }
        if (customersPanel != null) {
            customersPanel.detachFromServerEvents();
        }
        if (employeesPanel != null) {
            employeesPanel.detachFromServerEvents();
        }
        if (chatPanel != null) {
            chatPanel.detachFromServerEvents();
        }
        if (reportsPanel != null) {
            reportsPanel.detachFromServerEvents();
        }
    }

    /**
     * Ends the program in an orderly way: logs out, closes the connection and
     * only then exits.
     * <p>
     * Logging out matters even when the program is about to end. Without it the
     * server would keep the session until it noticed the dropped socket, and for
     * those few moments the employee could not log in from another computer.
     * </p>
     */
    private void closeApplication() {
        detachAllPanelsFromServerEvents();
        new Thread(() -> {
            try {
                loginController.logout();
            } catch (ChainStoreException ignoredFailure) {
                // The program is closing; there is nothing useful left to do.
            }
            ClientSession.getInstance().getConnection().disconnect();
            System.exit(0);
        }, "shutdown-worker").start();
    }
}
