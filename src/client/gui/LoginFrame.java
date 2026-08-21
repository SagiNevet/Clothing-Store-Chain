package client.gui;

import client.controller.LoginController;
import common.exception.ChainStoreException;
import common.model.Employee;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * The login screen, and the entry point of the client program.
 * <p>
 * <b>The most important idea in this class is what happens when the button is
 * pressed.</b> Sending the credentials to the server takes time: the socket has
 * to be opened, the request written, and the answer waited for. If that ran on
 * the Swing event dispatch thread the whole window would freeze - it would stop
 * repainting and stop reacting to the mouse - because that single thread is
 * also the one that draws everything.
 * </p>
 * <p>
 * So the work is split in the way every Swing program must:
 * </p>
 * <ol>
 *   <li>The Swing thread reads the two fields and disables the button.</li>
 *   <li>A background thread performs the network call and blocks as long as it
 *       needs to.</li>
 *   <li>{@link SwingUtilities#invokeLater} brings the result back to the Swing
 *       thread, which is the only thread allowed to open the next window or
 *       show an error.</li>
 * </ol>
 */
public class LoginFrame extends JFrame {

    /** Serialization version, required because Swing components are serializable. */
    private static final long serialVersionUID = 1L;

    /** The width of the window in pixels. */
    private static final int WINDOW_WIDTH = 460;

    /** The height of the window in pixels. */
    private static final int WINDOW_HEIGHT = 320;

    /** The number of characters the text fields are sized for. */
    private static final int FIELD_COLUMNS = 14;

    /** The size of the title at the top of the screen. */
    private static final float TITLE_FONT_SIZE = 18f;

    /** The field holding the employee number. */
    private final JTextField employeeNumberField = new JTextField(FIELD_COLUMNS);

    /** The field holding the password, which never shows the characters typed. */
    private final JPasswordField passwordField = new JPasswordField(FIELD_COLUMNS);

    /** The line at the bottom that reports what happened. */
    private final JLabel statusLabel = new JLabel(" ");

    /** The button that starts the login. */
    private final JButton loginButton = new JButton("Log in");

    /** The controller that talks to the server. */
    private final transient LoginController loginController = new LoginController();

    /**
     * Builds the login window.
     */
    public LoginFrame() {
        setTitle("Clothing Store Chain - Login");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(createTitleLabel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        loginButton.addActionListener(actionEvent -> startLogin());
        // Pressing Enter anywhere in the form activates the login button.
        getRootPane().setDefaultButton(loginButton);
    }

    /**
     * Builds the title shown at the top of the window.
     *
     * @return the title label
     */
    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Clothing Store Chain", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(18, 0, 10, 0));
        return titleLabel;
    }

    /**
     * Builds the two labelled fields and the login button.
     *
     * @return the panel holding the form
     */
    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints layoutRules = new GridBagConstraints();
        layoutRules.insets = new Insets(6, 8, 6, 8);
        layoutRules.anchor = GridBagConstraints.WEST;

        layoutRules.gridx = 0;
        layoutRules.gridy = 0;
        formPanel.add(new JLabel("Employee number:"), layoutRules);
        layoutRules.gridx = 1;
        formPanel.add(employeeNumberField, layoutRules);

        layoutRules.gridx = 0;
        layoutRules.gridy = 1;
        formPanel.add(new JLabel("Password:"), layoutRules);
        layoutRules.gridx = 1;
        formPanel.add(passwordField, layoutRules);

        layoutRules.gridx = 1;
        layoutRules.gridy = 2;
        formPanel.add(loginButton, layoutRules);

        return formPanel;
    }

    /**
     * Builds the bottom strip: the status line and the demonstration accounts.
     *
     * @return the footer panel
     */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(4, 12, 12, 12));

        statusLabel.setHorizontalAlignment(JLabel.CENTER);

        JLabel demoAccountsLabel = new JLabel("<html><center>Demonstration accounts, "
                + "password <b>Chain@2026</b><br>"
                + "1001 manager Tel Aviv &nbsp; 1002 cashier Tel Aviv<br>"
                + "2001 manager Jerusalem &nbsp; 2002 seller Jerusalem</center></html>",
                JLabel.CENTER);
        demoAccountsLabel.setEnabled(false);

        footerPanel.add(statusLabel, BorderLayout.NORTH);
        footerPanel.add(demoAccountsLabel, BorderLayout.SOUTH);
        return footerPanel;
    }

    /**
     * Starts a login attempt on a background thread.
     */
    private void startLogin() {
        String employeeNumber = employeeNumberField.getText().trim();
        // getPassword returns a char array rather than a String, so the password
        // is not left lying in the pool of interned strings.
        String password = new String(passwordField.getPassword());

        setFormEnabled(false);
        showStatus("Connecting to the server...", Color.DARK_GRAY);

        new Thread(() -> attemptLogin(employeeNumber, password), "login-worker").start();
    }

    /**
     * Performs the login against the server and reports the result back to the
     * Swing thread.
     *
     * @param employeeNumber the employee number typed by the user
     * @param password       the password typed by the user
     */
    private void attemptLogin(String employeeNumber, String password) {
        try {
            Employee authenticatedEmployee = loginController.login(employeeNumber, password);
            SwingUtilities.invokeLater(() -> openMainWindow(authenticatedEmployee));

        } catch (ChainStoreException loginFailure) {
            // Every refusal the server can produce arrives here with a message
            // already written for the user: a wrong password, an employee who is
            // already connected elsewhere, or a server that cannot be reached.
            SwingUtilities.invokeLater(() -> {
                showStatus(loginFailure.getMessage(), Color.RED.darker());
                setFormEnabled(true);
                passwordField.setText("");
            });
        }
    }

    /**
     * Closes the login window and opens the main window of the employee.
     *
     * @param authenticatedEmployee the employee returned by the server
     */
    private void openMainWindow(Employee authenticatedEmployee) {
        dispose();
        new MainFrame(authenticatedEmployee).setVisible(true);
    }

    /**
     * Turns the form on or off while a login is in progress, so the user cannot
     * press the button twice.
     *
     * @param enabled {@code true} to allow typing again
     */
    private void setFormEnabled(boolean enabled) {
        employeeNumberField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
    }

    /**
     * Writes a line in the status area.
     *
     * @param message the text to show
     * @param color   the colour of the text
     */
    private void showStatus(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    /**
     * Starts the client program.
     *
     * @param commandLineArguments not used
     */
    public static void main(String[] commandLineArguments) {
        // Makes the window look like a normal program of this operating system
        // instead of the default cross platform theme of Swing.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception themeFailure) {
            System.err.println("[Client] could not load the system theme, using the default one");
        }

        // Every Swing window must be created on the event dispatch thread, and
        // main runs on a different thread. This is the standard way to hand the
        // work over to Swing.
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
