package client.console;

import client.controller.ChatController;
import client.controller.ClientSession;
import client.controller.CustomerController;
import client.controller.EmployeeController;
import client.controller.InventoryController;
import client.controller.LoginController;
import client.controller.ReportController;
import client.net.ServerEventListener;
import common.exception.ChainStoreException;
import common.model.Branch;
import common.model.ChatMessage;
import common.model.ChatSessionInfo;
import common.model.Customer;
import common.model.CustomerType;
import common.model.Employee;
import common.model.PasswordPolicy;
import common.model.Product;
import common.model.ProductCategory;
import common.model.ReportRow;
import common.model.ReportType;
import common.model.Role;
import common.model.Sale;
import common.protocol.EventType;
import common.protocol.ProtocolKeys;
import common.protocol.ServerEvent;
import server.storage.StoragePaths;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Text-based console interface for the clothing store chain client.
 * <p>
 * Provides full CLI navigation for all 10 requirements of the course project:
 * authentication, branch inventory & sales, customer management with Strategy
 * pricing, employee administration & password policies, JSON and Word report
 * exports, real-time live events via Observer, and multi-branch chat with queue
 * management.
 * </p>
 */
public class ConsoleMain implements ServerEventListener {

    private final LoginController loginController = new LoginController();
    private final InventoryController inventoryController = new InventoryController();
    private final CustomerController customerController = new CustomerController();
    private final EmployeeController employeeController = new EmployeeController();
    private final ReportController reportController = new ReportController();
    private final ChatController chatController = new ChatController();

    private final Scanner scanner = new Scanner(System.in);
    private Employee currentEmployee;
    private volatile String activeChatSessionId = null;
    private volatile boolean inInteractiveChatMode = false;

    public static void main(String[] args) {
        ConsoleMain app = new ConsoleMain();
        app.run();
    }

    public void run() {
        printBanner();

        // Subscribe this console instance to live server events (Observer pattern)
        ClientSession.getInstance().getConnection().getEventDispatcher().subscribe(this);

        boolean running = true;
        while (running) {
            if (currentEmployee == null) {
                boolean loggedIn = handleLoginMenu();
                if (!loggedIn) {
                    running = false;
                }
            } else {
                handleMainMenu();
            }
        }

        System.out.println("\n[Console] Goodbye.");
    }

    private void printBanner() {
        System.out.println("========================================================================");
        System.out.println("       CLOTHING STORE CHAIN MANAGEMENT SYSTEM - CONSOLE CLIENT          ");
        System.out.println("       HIT - Java Algorithms & OOP Course - Summer 2026                 ");
        System.out.println("========================================================================");
    }

    private boolean handleLoginMenu() {
        System.out.println("\n--- LOGIN ---");
        System.out.println("Demo accounts: 1001 (Tel Aviv Shift Manager), 1002 (Tel Aviv Cashier)");
        System.out.println("               2001 (Jerusalem Shift Manager), 2002 (Jerusalem Seller)");
        System.out.println("Default Password for all: Chain@2026");
        System.out.println("Type 'exit' to quit.\n");

        System.out.print("Enter Employee Number: ");
        String employeeNumber = scanner.nextLine().trim();
        if (employeeNumber.equalsIgnoreCase("exit") || employeeNumber.equalsIgnoreCase("q")) {
            return false;
        }
        if (employeeNumber.isEmpty()) {
            return true;
        }

        System.out.print("Enter Password: ");
        String password = scanner.nextLine().trim();

        try {
            System.out.println("[Connecting to server...]");
            currentEmployee = loginController.login(employeeNumber, password);
            System.out.println("\n========================================================================");
            System.out.println("  LOGIN SUCCESSFUL!");
            System.out.println("  Welcome, " + currentEmployee.getFullName());
            System.out.println("  Employee No: " + currentEmployee.getEmployeeNumber()
                    + " | ID: " + currentEmployee.getIdNumber());
            System.out.println("  Role: " + currentEmployee.getRole().getDisplayName()
                    + " | Branch: " + currentEmployee.getBranch().getDisplayName());
            System.out.println("========================================================================");
            return true;
        } catch (ChainStoreException e) {
            System.err.println("\n[LOGIN FAILED] " + e.getMessage());
            return true;
        }
    }

    private void handleMainMenu() {
        boolean isShiftManager = currentEmployee.getRole().canManageEmployees();

        System.out.println("\n------------------------------------------------------------------------");
        System.out.println(" MAIN MENU - Branch: " + currentEmployee.getBranch().getDisplayName()
                + " | User: " + currentEmployee.getFullName()
                + " (" + currentEmployee.getRole().getDisplayName() + ")");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  1. View Branch Inventory");
        System.out.println("  2. Sell Product to Customer (Polymorphic Strategy Pricing)");
        System.out.println("  3. Restock Product (Purchase from Supplier)");
        System.out.println("  4. View All Customers (New / Returning / VIP)");
        System.out.println("  5. Register New Customer");
        System.out.println("  6. Update Customer Details");
        System.out.println("  7. Start Chat with another branch (Tel Aviv / Jerusalem)");
        System.out.println("  8. Enter Active Chat Room");
        System.out.println("  9. Close Active Chat Session");

        if (isShiftManager) {
            System.out.println("  --- Shift Manager / Admin Operations ---");
            System.out.println(" 10. Add New Product to Catalogue");
            System.out.println(" 11. View All Chain Employees");
            System.out.println(" 12. Add New Employee Account");
            System.out.println(" 13. View & Update Password Policy");
            System.out.println(" 14. Sales Report: By Branch");
            System.out.println(" 15. Sales Report: By Product / Category (JSON & Word Export)");
            System.out.println(" 16. View Active Chats & Join (Shift Manager)");
            System.out.println(" 17. View System Log Files (employees, customers, sales, chat)");
        }

        System.out.println("  0. Logout");
        System.out.print("\nSelect an option: ");

        String input = scanner.nextLine().trim();
        System.out.println();

        switch (input) {
            case "1":
                viewInventory();
                break;
            case "2":
                sellProduct();
                break;
            case "3":
                restockProduct();
                break;
            case "4":
                viewCustomers();
                break;
            case "5":
                registerCustomer();
                break;
            case "6":
                updateCustomer();
                break;
            case "7":
                startChat();
                break;
            case "8":
                enterChatRoom();
                break;
            case "9":
                closeChat();
                break;
            case "10":
                if (isShiftManager) addProduct(); else printInvalidOption();
                break;
            case "11":
                if (isShiftManager) viewEmployees(); else printInvalidOption();
                break;
            case "12":
                if (isShiftManager) addEmployee(); else printInvalidOption();
                break;
            case "13":
                if (isShiftManager) managePasswordPolicy(); else printInvalidOption();
                break;
            case "14":
                if (isShiftManager) salesByBranchReport(); else printInvalidOption();
                break;
            case "15":
                if (isShiftManager) salesByProductReport(); else printInvalidOption();
                break;
            case "16":
                if (isShiftManager) joinChatAsManager(); else printInvalidOption();
                break;
            case "17":
                if (isShiftManager) viewLogs(); else printInvalidOption();
                break;
            case "0":
                logout();
                break;
            default:
                printInvalidOption();
                break;
        }
    }

    private void printInvalidOption() {
        System.out.println("[Error] Invalid option selected. Please choose a number from the menu.");
    }

    // =========================================================================
    // 1. INVENTORY OPERATIONS
    // =========================================================================

    private void viewInventory() {
        try {
            List<Product> inventory = inventoryController.loadInventory();
            System.out.println("=== INVENTORY FOR BRANCH: " + currentEmployee.getBranch().getDisplayName() + " ===");
            System.out.printf("%-10s | %-25s | %-15s | %-10s | %-8s\n",
                    "Product ID", "Product Name", "Category", "Price (NIS)", "Stock");
            System.out.println("-----------------------------------------------------------------------------");
            for (Product p : inventory) {
                System.out.printf("%-10s | %-25s | %-15s | %-10.2f | %-8d\n",
                        p.getProductId(), p.getName(), p.getCategory().getDisplayName(),
                        p.getPrice(), p.getQuantity());
            }
            System.out.println("Total products: " + inventory.size());
        } catch (ChainStoreException e) {
            System.err.println("[Error] Could not load inventory: " + e.getMessage());
        }
    }

    private void sellProduct() {
        try {
            System.out.print("Enter Product ID to sell: ");
            String productId = scanner.nextLine().trim();

            System.out.print("Enter Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Enter Customer ID Number: ");
            String customerId = scanner.nextLine().trim();

            Sale sale = inventoryController.sellProduct(productId, quantity, customerId);

            System.out.println("\n[SALE SUCCESSFUL!]");
            System.out.println("--------------------------------------------------");
            System.out.println("Sale ID:          " + sale.getSaleId());
            System.out.println("Product:          " + sale.getProductName() + " (x" + sale.getQuantity() + ")");
            System.out.println("Catalogue Price:  " + String.format("%.2f NIS", sale.getUnitPrice() * sale.getQuantity()));
            System.out.println("Customer ID:      " + sale.getCustomerIdNumber());
            System.out.println("Customer Kind:    " + sale.getCustomerTypeAtSale());
            System.out.println("Discount Applied: " + String.format("%.2f NIS", sale.getDiscountAmount()));
            System.out.println("Final Paid Price: " + String.format("%.2f NIS", sale.getFinalPrice()));
            System.out.println("Timestamp:        " + sale.getSaleTime());
            System.out.println("--------------------------------------------------");
            System.out.println("The sale was processed polymorphically according to the customer strategy plan.");
        } catch (NumberFormatException e) {
            System.err.println("[Error] Invalid quantity number.");
        } catch (ChainStoreException e) {
            System.err.println("[Sale Refused] " + e.getMessage());
        }
    }

    private void restockProduct() {
        try {
            System.out.print("Enter Product ID to restock: ");
            String productId = scanner.nextLine().trim();

            System.out.print("Enter Quantity to add from supplier: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            Product updated = inventoryController.restockProduct(productId, quantity);
            System.out.println("[RESTOCK SUCCESSFUL] Product: " + updated.getName()
                    + " | New Stock Quantity: " + updated.getQuantity());
        } catch (NumberFormatException e) {
            System.err.println("[Error] Invalid quantity number.");
        } catch (ChainStoreException e) {
            System.err.println("[Restock Refused] " + e.getMessage());
        }
    }

    private void addProduct() {
        try {
            System.out.print("Enter New Product ID (e.g. P-500): ");
            String productId = scanner.nextLine().trim();

            System.out.print("Enter Product Name: ");
            String name = scanner.nextLine().trim();

            System.out.println("Categories: 1. SHIRTS  2. PANTS  3. SHOES  4. ACCESSORIES");
            System.out.print("Choose Category (1-4): ");
            String catChoice = scanner.nextLine().trim();
            ProductCategory category;
            switch (catChoice) {
                case "1": category = ProductCategory.SHIRTS; break;
                case "2": category = ProductCategory.PANTS; break;
                case "3": category = ProductCategory.SHOES; break;
                case "4": category = ProductCategory.ACCESSORIES; break;
                default: category = ProductCategory.SHIRTS; break;
            }

            System.out.print("Enter Unit Price (NIS): ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter Initial Stock Quantity: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());

            Product created = inventoryController.addProduct(productId, name, category, price, quantity);
            System.out.println("[PRODUCT ADDED] " + created.getName() + " (ID: " + created.getProductId() + ") stored in catalogue.");
        } catch (Exception e) {
            System.err.println("[Error] Could not add product: " + e.getMessage());
        }
    }

    // =========================================================================
    // 2. CUSTOMER OPERATIONS
    // =========================================================================

    private void viewCustomers() {
        try {
            List<Customer> customers = customerController.loadCustomers();
            System.out.println("=== CHAIN CUSTOMERS (Shared across all branches) ===");
            System.out.printf("%-12s | %-20s | %-14s | %-10s | %-9s | %-12s | %s\n",
                    "ID Number", "Full Name", "Phone", "Type", "Purchases", "Total Spent", "Discount Plan");
            System.out.println("----------------------------------------------------------------------------------------------------------------");
            for (Customer c : customers) {
                System.out.printf("%-12s | %-20s | %-14s | %-10s | %-9d | %-12.2f | %s\n",
                        c.getIdNumber(), c.getFullName(), c.getPhone(),
                        c.getCustomerType(), c.getPurchaseCount(), c.getTotalSpent(),
                        c.getPurchasePlanDescription());
            }
            System.out.println("Total registered customers: " + customers.size());
        } catch (ChainStoreException e) {
            System.err.println("[Error] Could not load customers: " + e.getMessage());
        }
    }

    private void registerCustomer() {
        try {
            System.out.print("Enter Customer ID Number (9 digits): ");
            String idNumber = scanner.nextLine().trim();

            System.out.print("Enter Customer Full Name: ");
            String fullName = scanner.nextLine().trim();

            System.out.print("Enter Customer Phone: ");
            String phone = scanner.nextLine().trim();

            Customer created = customerController.addCustomer(idNumber, fullName, phone);
            System.out.println("[CUSTOMER REGISTERED] " + created.getFullName()
                    + " | Type: " + created.getCustomerType() + " | " + created.getPurchasePlanDescription());
        } catch (ChainStoreException e) {
            System.err.println("[Registration Refused] " + e.getMessage());
        }
    }

    private void updateCustomer() {
        try {
            System.out.print("Enter Existing Customer ID Number: ");
            String idNumber = scanner.nextLine().trim();

            System.out.print("Enter Updated Full Name: ");
            String fullName = scanner.nextLine().trim();

            System.out.print("Enter Updated Phone: ");
            String phone = scanner.nextLine().trim();

            Customer updated = customerController.updateCustomer(idNumber, fullName, phone);
            System.out.println("[CUSTOMER UPDATED] " + updated.getFullName() + " (ID: " + updated.getIdNumber() + ")");
        } catch (ChainStoreException e) {
            System.err.println("[Update Refused] " + e.getMessage());
        }
    }

    // =========================================================================
    // 3. CHAT OPERATIONS
    // =========================================================================

    private void startChat() {
        Branch targetBranch = (currentEmployee.getBranch() == Branch.TEL_AVIV)
                ? Branch.JERUSALEM : Branch.TEL_AVIV;

        System.out.println("[Chat] Requesting connection to a free employee in " + targetBranch.getDisplayName() + "...");
        try {
            ChatSessionInfo sessionInfo = chatController.requestChat(targetBranch);
            activeChatSessionId = sessionInfo.getSessionId();
            System.out.println("\n[CHAT SESSION OPENED]");
            System.out.println("Session ID: " + activeChatSessionId);
            System.out.println("Talking with: " + sessionInfo.getPartnerName()
                    + " (" + sessionInfo.getPartnerBranch().getDisplayName() + ")");
            enterChatRoom();
        } catch (ChainStoreException e) {
            System.out.println("\n[CHAT STATUS] " + e.getMessage());
            System.out.println("If nobody is free, your request was queued on the server.");
            System.out.println("You will receive an automatic live notification when an employee becomes free.");
        }
    }

    private void enterChatRoom() {
        if (activeChatSessionId == null) {
            System.out.println("[Chat] No active chat session. Start a chat first (option 7).");
            return;
        }

        System.out.println("\n========================================================================");
        System.out.println("  ACTIVE CHAT ROOM - Session: " + activeChatSessionId);
        System.out.println("  Type your message and press ENTER to send.");
        System.out.println("  Type '/exit' or 'exit' to return to Main Menu (keeps session open).");
        System.out.println("========================================================================");

        inInteractiveChatMode = true;
        try {
            while (inInteractiveChatMode && activeChatSessionId != null) {
                System.out.print("> ");
                String line = scanner.nextLine().trim();
                if (line.equalsIgnoreCase("/exit") || line.equalsIgnoreCase("exit")) {
                    break;
                }
                if (line.equalsIgnoreCase("/close") || line.equalsIgnoreCase("close")) {
                    closeChat();
                    break;
                }
                if (!line.isEmpty()) {
                    try {
                        chatController.sendMessage(activeChatSessionId, line);
                    } catch (ChainStoreException e) {
                        System.err.println("[Failed to send message] " + e.getMessage());
                    }
                }
            }
        } finally {
            inInteractiveChatMode = false;
        }
    }

    private void closeChat() {
        if (activeChatSessionId == null) {
            System.out.println("[Chat] No active chat session to close.");
            return;
        }
        try {
            chatController.closeChat(activeChatSessionId);
            System.out.println("[Chat] Session " + activeChatSessionId + " closed successfully.");
            activeChatSessionId = null;
        } catch (ChainStoreException e) {
            System.err.println("[Error closing chat] " + e.getMessage());
            activeChatSessionId = null;
        }
    }

    private void joinChatAsManager() {
        try {
            List<ChatSessionInfo> openChats = chatController.loadOpenChats();
            if (openChats.isEmpty()) {
                System.out.println("[Chat Admin] No active chat conversations currently in progress.");
                return;
            }

            System.out.println("=== ACTIVE CHAIN CHAT SESSIONS ===");
            for (int i = 0; i < openChats.size(); i++) {
                ChatSessionInfo info = openChats.get(i);
                System.out.println((i + 1) + ". Session ID: " + info.getSessionId()
                        + " | Initiator: " + info.getInitiatorName() + " (" + info.getInitiatorBranch() + ")"
                        + " <-> Partner: " + info.getPartnerName() + " (" + info.getPartnerBranch() + ")");
            }

            System.out.print("Select session number to join (or 0 to cancel): ");
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice <= 0 || choice > openChats.size()) {
                return;
            }

            ChatSessionInfo chosen = openChats.get(choice - 1);
            List<ChatMessage> history = chatController.joinChat(chosen.getSessionId());
            activeChatSessionId = chosen.getSessionId();

            System.out.println("\n[JOINED CHAT SESSION: " + activeChatSessionId + "]");
            System.out.println("--- Conversation History Prior to Joining ---");
            for (ChatMessage msg : history) {
                System.out.println("[" + msg.getSentAt() + "] " + msg.getSenderFullName() + ": " + msg.getContent());
            }
            System.out.println("----------------------------------------------");

            enterChatRoom();
        } catch (Exception e) {
            System.err.println("[Error joining chat] " + e.getMessage());
        }
    }

    // =========================================================================
    // 4. EMPLOYEE & ADMIN OPERATIONS (Shift Manager)
    // =========================================================================

    private void viewEmployees() {
        try {
            List<Employee> employees = employeeController.loadEmployees();
            System.out.println("=== CHAIN EMPLOYEES ===");
            System.out.printf("%-12s | %-18s | %-12s | %-14s | %-15s | %-12s | %s\n",
                    "Employee No", "Full Name", "ID Number", "Phone", "Bank Account", "Branch", "Role");
            System.out.println("---------------------------------------------------------------------------------------------------------");
            for (Employee emp : employees) {
                System.out.printf("%-12s | %-18s | %-12s | %-14s | %-15s | %-12s | %s\n",
                        emp.getEmployeeNumber(), emp.getFullName(), emp.getIdNumber(),
                        emp.getPhone(), emp.getBankAccountNumber(),
                        emp.getBranch().getDisplayName(), emp.getRole().getDisplayName());
            }
            System.out.println("Total employees: " + employees.size());
        } catch (ChainStoreException e) {
            System.err.println("[Error] Could not load employees: " + e.getMessage());
        }
    }

    private void addEmployee() {
        try {
            System.out.print("Enter Employee Number (e.g. 1003): ");
            String empNo = scanner.nextLine().trim();

            System.out.print("Enter Full Name: ");
            String name = scanner.nextLine().trim();

            System.out.print("Enter National ID: ");
            String idNumber = scanner.nextLine().trim();

            System.out.print("Enter Phone: ");
            String phone = scanner.nextLine().trim();

            System.out.print("Enter Bank Account: ");
            String bankAccount = scanner.nextLine().trim();

            System.out.println("Branch: 1. Tel Aviv  2. Jerusalem");
            System.out.print("Choose branch (1-2): ");
            Branch branch = scanner.nextLine().trim().equals("2") ? Branch.JERUSALEM : Branch.TEL_AVIV;

            System.out.println("Role: 1. Shift Manager  2. Cashier  3. Seller");
            System.out.print("Choose role (1-3): ");
            String roleChoice = scanner.nextLine().trim();
            Role role;
            switch (roleChoice) {
                case "1": role = Role.SHIFT_MANAGER; break;
                case "2": role = Role.CASHIER; break;
                default: role = Role.SELLER; break;
            }

            System.out.print("Enter Password for Account: ");
            String password = scanner.nextLine().trim();

            Employee created = employeeController.addEmployee(empNo, name, idNumber, phone,
                    bankAccount, branch, role, password);
            System.out.println("[EMPLOYEE CREATED] " + created.getFullName() + " (No: " + created.getEmployeeNumber()
                    + ") created successfully on server.");
        } catch (ChainStoreException e) {
            System.err.println("[Creation Refused] " + e.getMessage());
        }
    }

    private void managePasswordPolicy() {
        try {
            PasswordPolicy policy = employeeController.loadPasswordPolicy();
            System.out.println("=== CURRENT PASSWORD POLICY ===");
            System.out.println("1. Minimum Length:             " + policy.getMinimumLength());
            System.out.println("2. Require Uppercase Letter:   " + policy.isUpperCaseLetterRequired());
            System.out.println("3. Require Lowercase Letter:   " + policy.isLowerCaseLetterRequired());
            System.out.println("4. Require Digit:              " + policy.isDigitRequired());
            System.out.println("5. Require Special Character:  " + policy.isSpecialCharacterRequired());
            System.out.println("----------------------------------");
            System.out.print("Do you want to update the policy? (y/n): ");
            String answer = scanner.nextLine().trim();
            if (!answer.equalsIgnoreCase("y")) {
                return;
            }

            System.out.print("Enter Minimum Length (e.g. 6): ");
            int minLen = Integer.parseInt(scanner.nextLine().trim());

            System.out.print("Require Uppercase? (true/false): ");
            boolean reqUpper = Boolean.parseBoolean(scanner.nextLine().trim());

            System.out.print("Require Lowercase? (true/false): ");
            boolean reqLower = Boolean.parseBoolean(scanner.nextLine().trim());

            System.out.print("Require Digit? (true/false): ");
            boolean reqDigit = Boolean.parseBoolean(scanner.nextLine().trim());

            System.out.print("Require Special Char? (true/false): ");
            boolean reqSpecial = Boolean.parseBoolean(scanner.nextLine().trim());

            PasswordPolicy newPolicy = new PasswordPolicy(minLen, reqDigit, reqUpper, reqLower, reqSpecial);
            employeeController.updatePasswordPolicy(newPolicy);
            System.out.println("[POLICY UPDATED] New password policy is now in effect.");
        } catch (Exception e) {
            System.err.println("[Error] Could not update policy: " + e.getMessage());
        }
    }

    // =========================================================================
    // 5. REPORTS OPERATIONS (Shift Manager)
    // =========================================================================

    private void salesByBranchReport() {
        try {
            List<ReportRow> rows = reportController.buildReport(ReportType.SALES_BY_BRANCH, null, null);
            System.out.println("=== SALES BY BRANCH REPORT ===");
            System.out.printf("%-15s | %-10s | %-10s | %-14s | %-14s\n",
                    "Branch", "Sales Count", "Items Sold", "Revenue (NIS)", "Discounts (NIS)");
            System.out.println("--------------------------------------------------------------------------");
            for (ReportRow row : rows) {
                System.out.printf("%-15s | %-10d | %-10d | %-14.2f | %-14.2f\n",
                        row.getGroupName(), row.getNumberOfSales(), row.getItemsSold(),
                        row.getTotalRevenue(), row.getTotalDiscount());
            }

            askAndExportReport(ReportType.SALES_BY_BRANCH, null, null);
        } catch (ChainStoreException e) {
            System.err.println("[Report Error] " + e.getMessage());
        }
    }

    private void salesByProductReport() {
        try {
            List<ReportRow> rows = reportController.buildReport(ReportType.SALES_BY_PRODUCT, null, null);
            System.out.println("=== SALES BY PRODUCT REPORT ===");
            System.out.printf("%-20s | %-10s | %-10s | %-14s | %-14s\n",
                    "Product", "Sales Count", "Items Sold", "Revenue (NIS)", "Discounts (NIS)");
            System.out.println("--------------------------------------------------------------------------");
            for (ReportRow row : rows) {
                System.out.printf("%-20s | %-10d | %-10d | %-14.2f | %-14.2f\n",
                        row.getGroupName(), row.getNumberOfSales(), row.getItemsSold(),
                        row.getTotalRevenue(), row.getTotalDiscount());
            }

            askAndExportReport(ReportType.SALES_BY_PRODUCT, null, null);
        } catch (ChainStoreException e) {
            System.err.println("[Report Error] " + e.getMessage());
        }
    }

    private void askAndExportReport(ReportType type, List<String> productFilter, List<ProductCategory> categoryFilter) {
        System.out.println("\nExport Options: 1. Word Document (.rtf)  2. JSON File (.json)  0. Skip");
        System.out.print("Choose export format (0-2): ");
        String choice = scanner.nextLine().trim();
        if (choice.equals("1")) {
            try {
                String path = reportController.exportReport(type, productFilter, categoryFilter, "Word");
                System.out.println("[WORD EXPORT SUCCESSFUL] File generated at: " + path);
            } catch (ChainStoreException e) {
                System.err.println("[Export Failed] " + e.getMessage());
            }
        } else if (choice.equals("2")) {
            try {
                String path = reportController.exportReport(type, productFilter, categoryFilter, "JSON");
                System.out.println("[JSON EXPORT SUCCESSFUL] File generated at: " + path);
                File jsonFile = new File(path);
                if (jsonFile.exists()) {
                    System.out.println("--- Generated JSON Content ---");
                    System.out.println(new String(Files.readAllBytes(jsonFile.toPath()), StandardCharsets.UTF_8));
                    System.out.println("------------------------------");
                }
            } catch (Exception e) {
                System.err.println("[Export Failed] " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 6. SYSTEM LOGS (Shift Manager)
    // =========================================================================

    private void viewLogs() {
        System.out.println("=== SYSTEM LOG FILES ===");
        System.out.println("1. employees.log (Employee account registrations & modifications)");
        System.out.println("2. customers.log (Customer registrations & status upgrades)");
        System.out.println("3. sales.log     (Sales & Restock operations)");
        System.out.println("4. chat.log      (Chat session metadata & details)");
        System.out.println("0. Return to Main Menu");
        System.out.print("Select log to view (0-4): ");
        String choice = scanner.nextLine().trim();

        String fileName;
        switch (choice) {
            case "1": fileName = "employees.log"; break;
            case "2": fileName = "customers.log"; break;
            case "3": fileName = "sales.log"; break;
            case "4": fileName = "chat.log"; break;
            default: return;
        }

        File logFile = new File(StoragePaths.LOGS_DIRECTORY, fileName);
        if (!logFile.exists() || logFile.length() == 0) {
            System.out.println("[LogViewer] File " + fileName + " is currently empty or has not been created yet.");
            return;
        }

        System.out.println("\n--- CONTENTS OF " + fileName + " ---");
        try {
            List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
            int start = Math.max(0, lines.size() - 25);
            for (int i = start; i < lines.size(); i++) {
                System.out.println(lines.get(i));
            }
            if (lines.size() > 25) {
                System.out.println("... (showing last 25 lines out of " + lines.size() + ")");
            }
        } catch (IOException e) {
            System.err.println("[Error reading log file] " + e.getMessage());
        }
        System.out.println("----------------------------------\n");
    }

    // =========================================================================
    // 7. LOGOUT
    // =========================================================================

    private void logout() {
        try {
            loginController.logout();
            System.out.println("[Logout] Logged out successfully from server.");
        } catch (ChainStoreException e) {
            System.err.println("[Logout warning] " + e.getMessage());
        } finally {
            currentEmployee = null;
            activeChatSessionId = null;
        }
    }

    // =========================================================================
    // OBSERVER PATTERN - ServerEventListener Implementation
    // =========================================================================

    @Override
    public void onServerEvent(ServerEvent event) {
        EventType type = event.getEventType();
        switch (type) {
            case INVENTORY_UPDATED:
                System.out.println("\n>>> [LIVE EVENT: Observer] Inventory updated for branch "
                        + event.getPayload(ProtocolKeys.BRANCH));
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CUSTOMERS_UPDATED:
                System.out.println("\n>>> [LIVE EVENT: Observer] Customer list updated in chain database.");
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case EMPLOYEES_UPDATED:
                System.out.println("\n>>> [LIVE EVENT: Observer] Employee list updated by manager.");
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CHAT_INVITE:
                String inviteSessionId = (String) event.getPayload(ProtocolKeys.CHAT_SESSION_ID);
                Employee initiator = (Employee) event.getPayload(ProtocolKeys.EMPLOYEE);
                activeChatSessionId = inviteSessionId;
                String senderName = (initiator != null) ? initiator.getFullName() : "Colleague";
                String branchName = (initiator != null) ? initiator.getBranch().getDisplayName() : "Other branch";
                System.out.println("\n>>> [LIVE CHAT INVITE] " + senderName + " from "
                        + branchName + " opened a chat with you! (Session: " + inviteSessionId + ")");
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CHAT_MESSAGE:
                ChatMessage message = (ChatMessage) event.getPayload(ProtocolKeys.CHAT_MESSAGE);
                if (message != null) {
                    if (currentEmployee != null && !message.getSenderEmployeeNumber().equals(currentEmployee.getEmployeeNumber())) {
                        System.out.println("\n[" + message.getSenderFullName() + "]: " + message.getContent());
                    }
                }
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CHAT_PEER_AVAILABLE:
                Branch freeBranch = (Branch) event.getPayload(ProtocolKeys.BRANCH);
                System.out.println("\n>>> [LIVE CHAT QUEUE NOTIFICATION] An employee from "
                        + (freeBranch != null ? freeBranch.getDisplayName() : "the target branch")
                        + " is now free! You can now call back using Option 7 in the Chat menu.");
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CHAT_MANAGER_JOINED:
                Employee manager = (Employee) event.getPayload(ProtocolKeys.EMPLOYEE);
                String managerName = (manager != null) ? manager.getFullName() : "Shift Manager";
                System.out.println("\n>>> [CHAT NOTICE] Shift Manager " + managerName + " joined the conversation.");
                if (inInteractiveChatMode) System.out.print("> ");
                break;

            case CHAT_CLOSED:
                System.out.println("\n>>> [CHAT NOTICE] The conversation was closed.");
                activeChatSessionId = null;
                if (inInteractiveChatMode) {
                    inInteractiveChatMode = false;
                }
                break;

            case FORCED_LOGOUT:
                System.out.println("\n>>> [ALERT] Disconnected from server.");
                currentEmployee = null;
                activeChatSessionId = null;
                break;

            default:
                break;
        }
    }
}
