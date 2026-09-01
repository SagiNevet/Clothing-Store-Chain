# Clothing Store Chain Management System

A Java client-server console-based system for managing a clothing store retail chain, developed for the Java Algorithms and Object-Oriented Programming course at HIT (Summer 2026).

---

## Team

| # | Name | ID |
|---|---|---|
| 1 | Teva Jalink | 208181446 |
| 2 | Michelle Aizikovich | 317868172 |
| 3 | Sagi Nevet | 314618166 |
| 4 | [FILL IN] | [FILL IN] |
| 5 | [FILL IN] | [FILL IN] |
| 6 | [FILL IN] | [FILL IN] |

---

## Requirements and Prerequisites

- **Java Development Kit (JDK):** Tested and verified on **JDK 11** (compatible with JDK 11+).
- **Zero External Dependencies:** Built using standard Java SE libraries only. No Maven, no Gradle, no Spring, and no third-party runtime frameworks.
- **Testing Dependency:** JUnit 5 standalone runner is included under `lib/junit-platform-console-standalone-1.10.2.jar` for running automated unit and integration tests only. The application itself compiles and runs using pure JDK.

---

## System Architecture Overview

The project is structured in a clean, decoupled **Client-Server Architecture**:

- **`src/common/`**: Shared domain models (`Employee`, `Customer`, `Product`, `Sale`), custom exception hierarchy (`ChainStoreException`), serialization protocol objects (`Request`, `Response`, `ServerEvent`), and utility classes (`PasswordHasher`, `AppConfig`, `IdGenerator`).
- **`src/server/`**: Multi-threaded TCP server (`ChainServer`, `ClientHandler`), business command pattern dispatcher (`CommandFactory`), thread pool executor (`BusinessTaskExecutor`), domain services, chat queue manager (`ChatQueueManager`), report exporters (JSON and Word RTF), and binary file persistence repositories.
- **`src/client/`**: Interactive text-based console interface (`ConsoleMain`), domain controllers (`InventoryController`, `CustomerController`, `ChatController`, etc.), asynchronous event dispatcher (`ClientEventDispatcher`), and socket connection layer (`ServerConnection`). A Swing GUI client is also available as an alternative frontend (`LoginFrame`).
- **`src/test/`**: 90 comprehensive JUnit 5 tests verifying business logic, concurrency, persistence, socket communications, and design patterns.

---

## How to Run

All commands and scripts should be executed from the project root directory.

### 1. Compile the System

Compiles all source files into the `out/` folder:

```bat
compile.bat
```

*Manual backup command:*
```bash
mkdir out
javac -encoding UTF-8 -d out -sourcepath src src/server/core/ChainServer.java src/client/console/ConsoleMain.java
```

### 2. Start the Server

Start the server **before** launching clients. By default, the server listens on port **5000** (configured in `config.properties`):

```bat
run_server.bat
```

*Manual backup command:*
```bash
java -cp out server.core.ChainServer
```

*Note:* On its initial startup, the server automatically creates the runtime directories (`data/`, `logs/`, `reports/`) and seeds initial demonstration employees and branch inventories (`DataSeeder`).

### 3. Start the Console Clients (Run in 2 Separate Windows)

To demonstrate multi-branch operations and inter-branch live chat, run the client script **twice** in separate terminal windows:

```bat
run_client.bat
```

*Manual backup command (execute once per terminal window):*
```bash
java -cp out client.console.ConsoleMain
```

*(Optional Swing GUI Client: `run_client_gui.bat` or `java -cp out client.gui.LoginFrame`)*

---

## Demonstration Accounts

All demonstration accounts are seeded with the default password: **`Chain@2026`** (passwords are securely hashed using SHA-256 with individual salts).

| Employee No | Password | Full Name | Role | Branch | Privileges |
|---|---|---|---|---|---|
| **1001** | `Chain@2026` | Maya Shir | Shift Manager (Admin) | Tel Aviv | Full access: Inventory, Customers, Chat, Employees, Password Policy, Reports (JSON/Word), Join active chats |
| **1002** | `Chain@2026` | Ron Levi | Cashier | Tel Aviv | Branch inventory, sales, customer registration, branch chat |
| **2001** | `Chain@2026` | Avi Dagan | Shift Manager (Admin) | Jerusalem | Full admin access for Jerusalem branch |
| **2002** | `Chain@2026` | Tamar Ben Ari | Seller | Jerusalem | Branch inventory, sales, customer registration, branch chat |

---

## Step-by-Step Feature Walkthrough (All 10 Assignment Requirements)

### 1. Client-Server Architecture & Authentication (Requirements 1, 2)
- Start the server, then launch a client.
- Enter employee number `1002` and password `Chain@2026`. The client connects over TCP sockets using serialized objects.
- Attempting to log in with an invalid password or non-existent user gives a clear refusal message.

### 2. Role-Based Navigation (Requirement 4)
- **Cashier/Seller (e.g. 1002):** Shows options 1–9 (Inventory, Sales, Customers, Chat). Administrative options are hidden.
- **Shift Manager (e.g. 1001):** Shows extended options 10–17 (Add Products, View/Add Employees, Password Policy, Branch/Product Reports, Join Active Chats, View Logs).

### 3. Branch Inventory & Restock (Requirement 5)
- **View Inventory (Option 1):** Displays products and current stock specifically for the logged-in branch. Tel Aviv and Jerusalem maintain isolated stock levels.
- **Restock Product (Option 3):** Simulates purchasing inventory from suppliers and increases product quantities.

### 4. Customer Management & Strategy Pricing (Requirement 6)
- **Customer Tiers:**
  - `NewCustomer`: 10% welcome discount on the first purchase.
  - `ReturningCustomer`: 5% permanent loyalty discount; 8% discount when purchasing 3 or more items.
  - `VipCustomer`: 15% permanent discount; 20% discount on orders exceeding 500 NIS.
- **Polymorphic Execution:** The sale calculation executes `customer.calculateFinalPrice(...)` without any `if/switch` checks on customer type.
- **Automatic Tier Upgrade:** When thresholds are reached (1 purchase for Returning; 5 purchases or 1,000 NIS total spent for VIP), `CustomerFactory.upgradeIfNeeded()` automatically instantiates the upgraded subclass while preserving purchase history.
- **Live Sync (Observer):** Registering or updating a customer in one client immediately broadcasts a `CUSTOMERS_UPDATED` event to all connected clients across the entire chain.

### 5. Sales Reports & Export in JSON / Word (Requirement 7)
- Log in as Shift Manager (`1001`).
- **Sales by Branch (Option 14):** Aggregates sales count, items sold, revenue, and discounts per branch.
- **Sales by Product (Option 15):** Generates product-level breakdown.
- **JSON Export:** Writes structured JSON report files to `reports/` and prints the output directly in the console.
- **Word Export:** Generates standard native Rich Text Format (`.rtf`) documents in `reports/`, opening seamlessly in Microsoft Word without format warnings.

### 6. Employee Management & Password Policy (Requirements 3, 8)
- **View/Add Employees (Options 11, 13):** Lists and creates employee accounts with full details (Name, National ID, Phone, Bank Account, Branch, Role).
- **Password Policy (Option 12):** Shift managers can view and configure policy rules (minimum length, uppercase, lowercase, digit, special character). New employee passwords are validated against this policy before account creation.

### 7. Inter-Branch Chat & Queue Management (Requirement 9)
- **Live 1-on-1 Chat (Option 7 & 8):** Tel Aviv employee requests a chat with Jerusalem; the free Jerusalem employee receives an instant invitation and can enter the interactive chat room.
- **Queue & Callback Notification:** If all employees in the target branch are busy, the request is placed into a server-side FIFO queue. When an employee becomes free, the server triggers a live `CHAT_PEER_AVAILABLE` notification to the requester.
- **Shift Manager Join (Option 16):** Shift managers can view in-progress conversations and join them, automatically receiving the conversation history.
- **Single Active Session Constraint:** An employee cannot participate in multiple simultaneous chats or log in concurrently from multiple terminals.

### 8. System Activity Logs (Requirement 10)
- Shift managers can inspect real-time log files directly via **Option 17**:
  - `logs/employees.log`: Account creations and updates.
  - `logs/customers.log`: Customer registrations and automatic tier promotions.
  - `logs/sales.log`: Sales transactions and inventory restock operations.
  - `logs/chat.log`: Metadata of inter-branch chat sessions (who talked to whom, branch, duration).

---

## Design Patterns and Technical Implementation

| Design Pattern | Implementation Location | Purpose and Technical Value |
|---|---|---|
| **Observer** | `EventPublisher` (Server) / `ClientEventDispatcher`, `ServerEventListener` (Client) | Enables real-time server push events (inventory changes, customer updates, chat invitations) without client polling. |
| **Strategy** | `Customer` hierarchy (`NewCustomer`, `ReturningCustomer`, `VipCustomer`) | Encapsulates pricing algorithms polymorphically inside customer subclasses. Adding a new customer tier requires no modifications to the sale processing code. |
| **Factory** | `CustomerFactory`, `CommandFactory`, `ReportExporter` selection | Centralizes object creation and type upgrades based on business rules and action enums. |
| **Command** | `server.command.Command` interface and 19 action implementations | Encapsulates each client request into a command object, centralizing permission checking, error handling, and audit logging in `ClientHandler`. |
| **Singleton** | `SessionManager`, `LogManager`, `ClientRegistry`, `ServerContext`, `AppConfig`, `ChatService`, `ChatQueueManager` | Guarantees single points of state management (e.g. preventing duplicate logins and coordinating thread pools). |
| **Monitor / Queue** | `ChatQueueManager` using `wait()` / `notifyAll()` and synchronized blocks | Coordinates thread waiting when finding free chat partners, falling back to a FIFO queue without blocking worker threads indefinitely. |

---

## Concurrency & Threading Mechanisms

1. **Accept Loop:** `ChainServer` runs a non-blocking `accept()` loop spawning a dedicated `ClientHandler` thread per connection.
2. **Fixed Business Thread Pool:** `BusinessTaskExecutor` bounds concurrent business operations via `Executors.newFixedThreadPool(4)` (configurable via `business.threadPool.size`).
3. **Granular Synchronization:** Fine-grained synchronization on individual `Product` instances during sales, and separate lock objects per `LogCategory` in `LogWriter` to prevent bottlenecks across unrelated files.
4. **Thread-Safe Collections:** Extensive use of `ConcurrentHashMap` and `CopyOnWriteArrayList` for active client sessions and event listeners.
5. **Clean Thread Shutdown:** Volatile boolean flags (`isRunning`, `isHandlerRunning`) ensure graceful resource cleanup in `finally` blocks.
6. **Parallelism Demonstration:** Setting `demo.taskDelayMillis=1500` in `config.properties` allows observing simultaneous interleaved operations on different pool threads directly in the server console.

---

## Automated Tests (JUnit 5)

To run the complete suite of **90 automated unit and integration tests**:

```bat
run_tests.bat
```

Tests run in an isolated workspace (`test-workspace/`) to guarantee that test data never pollutes live demonstration files.

---

## Runtime Data Files

All system data is stored locally via Java Object Serialization and UTF-8 text logs:

```
data/
  employees.dat              # Serialized employee accounts & password hashes
  customers.dat              # Shared chain customer records
  sales.dat                  # Permanent transaction history
  password_policy.dat        # System password validation rules
  inventory_TEL_AVIV.dat     # Tel Aviv branch stock
  inventory_JERUSALEM.dat    # Jerusalem branch stock
logs/
  employees.log, customers.log, sales.log, chat.log
reports/
  *.rtf, *.json              # Generated Word RTF and JSON export files
```

To reset the system to fresh demonstration data, simply delete the `data/` and `logs/` folders; the server will re-seed all initial records upon the next startup.
