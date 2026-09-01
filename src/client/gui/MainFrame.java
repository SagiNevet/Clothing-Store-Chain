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

public class MainFrame extends JFrame implements ServerEventListener {

    private static final long serialVersionUID = 1L;

    private static final int WINDOW_WIDTH = 900;

    private static final int WINDOW_HEIGHT = 600;

    private static final float HEADER_FONT_SIZE = 16f;

    private final transient Employee employee;

    private final transient LoginController loginController = new LoginController();

    private InventoryPanel inventoryPanel;

    private CustomersPanel customersPanel;

    private EmployeesPanel employeesPanel;

    private ChatPanel chatPanel;

    private ReportsPanel reportsPanel;

    public MainFrame(Employee employee) {
        this.employee = employee;

        setTitle("Clothing Store Chain - " + employee.getBranch().getDisplayName()
                + " - " + employee.getFullName());
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        
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

    @Override
    public void onServerEvent(ServerEvent event) {
        if (event.getEventType() == EventType.FORCED_LOGOUT) {
            JOptionPane.showMessageDialog(this,
                    "The server closed your session.",
                    "Session closed", JOptionPane.WARNING_MESSAGE);
            returnToLoginScreen();
        }
    }

    private void performLogout() {

        new Thread(() -> {
            try {
                loginController.logout();
            } catch (ChainStoreException logoutFailure) {
                System.err.println("[Client] logout failed: " + logoutFailure.getMessage());
            }
            SwingUtilities.invokeLater(this::returnToLoginScreen);
        }, "logout-worker").start();
    }

    private void returnToLoginScreen() {
        detachAllPanelsFromServerEvents();
        ClientSession.getInstance().clearCurrentEmployee();
        dispose();
        new LoginFrame().setVisible(true);
    }

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

    private void closeApplication() {
        detachAllPanelsFromServerEvents();
        new Thread(() -> {
            try {
                loginController.logout();
            } catch (ChainStoreException ignoredFailure) {
            }
            ClientSession.getInstance().getConnection().disconnect();
            System.exit(0);
        }, "shutdown-worker").start();
    }
}
