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

public class LoginFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final int WINDOW_WIDTH = 460;

    private static final int WINDOW_HEIGHT = 320;

    private static final int FIELD_COLUMNS = 14;

    private static final float TITLE_FONT_SIZE = 18f;

    private final JTextField employeeNumberField = new JTextField(FIELD_COLUMNS);

    private final JPasswordField passwordField = new JPasswordField(FIELD_COLUMNS);

    private final JLabel statusLabel = new JLabel(" ");

    private final JButton loginButton = new JButton("Log in");

    private final transient LoginController loginController = new LoginController();

    public LoginFrame() {
        setTitle("Clothing Store Chain - Login");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        add(createTitleLabel(), BorderLayout.NORTH);
        add(createFormPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        loginButton.addActionListener(actionEvent -> startLogin());
        
        getRootPane().setDefaultButton(loginButton);
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel("Clothing Store Chain", JLabel.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, TITLE_FONT_SIZE));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(18, 0, 10, 0));
        return titleLabel;
    }

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

    private void startLogin() {
        String employeeNumber = employeeNumberField.getText().trim();
        
        String password = new String(passwordField.getPassword());

        setFormEnabled(false);
        showStatus("Connecting to the server...", Color.DARK_GRAY);

        new Thread(() -> attemptLogin(employeeNumber, password), "login-worker").start();
    }

    private void attemptLogin(String employeeNumber, String password) {
        try {
            Employee authenticatedEmployee = loginController.login(employeeNumber, password);
            SwingUtilities.invokeLater(() -> openMainWindow(authenticatedEmployee));

        } catch (ChainStoreException loginFailure) {

            SwingUtilities.invokeLater(() -> {
                showStatus(loginFailure.getMessage(), Color.RED.darker());
                setFormEnabled(true);
                passwordField.setText("");
            });
        }
    }

    private void openMainWindow(Employee authenticatedEmployee) {
        dispose();
        new MainFrame(authenticatedEmployee).setVisible(true);
    }

    private void setFormEnabled(boolean enabled) {
        employeeNumberField.setEnabled(enabled);
        passwordField.setEnabled(enabled);
        loginButton.setEnabled(enabled);
    }

    private void showStatus(String message, Color color) {
        statusLabel.setForeground(color);
        statusLabel.setText(message);
    }

    public static void main(String[] commandLineArguments) {
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception themeFailure) {
            System.err.println("[Client] could not load the system theme, using the default one");
        }

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
