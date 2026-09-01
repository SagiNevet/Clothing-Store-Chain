# Clothing Store Chain Management System

A distributed Java client, server enterprise platform for managing multi branch retail clothing operations.

***

## Engineering Team

| # | Name | ID |
|---|---|---|
| 1 | Teva Jalink | 208181446 |
| 2 | Michelle Aizikovich | 317868172 |
| 3 | Sagi Nevet | 314618166 |
| 4 | Roey Biran | 318871035 |
| 5 | Noam Shani | 315758839 |
| 6 |  |  |

***

## System Prerequisites

* **Java Development Kit (JDK):** Verified on **JDK 11** (fully compatible with JDK 11 and newer).
* **Zero External Runtime Dependencies:** Built using standard Java SE libraries only, with no Maven, Gradle, Spring, or third party frameworks.
* **Automated Test Runner:** Includes the standalone JUnit platform console runner in `lib/junit-platform-console-standalone-1.10.2.jar` for executing automated test suites. The core application compiles and executes using pure JDK.

***

## System Architecture

The project implements a decoupled **Client Server Architecture**:

* **`src/common/`**: Shared domain models (`Employee`, `Customer`, `Product`, `Sale`), custom checked exceptions (`ChainStoreException`), network protocol objects (`Request`, `Response`, `ServerEvent`), and utilities (`PasswordHasher`, `AppConfig`, `IdGenerator`).
* **`src/server/`**: Multi threaded TCP server (`ChainServer`, `ClientHandler`), business command dispatcher (`CommandFactory`), thread pool executor (`BusinessTaskExecutor`), domain services, inter branch chat queue manager (`ChatQueueManager`), report exporters (JSON and Word RTF), and binary file persistence repositories.
* **`src/client/`**: Interactive text based console interface (`ConsoleMain`), domain controllers (`InventoryController`, `CustomerController`, `ChatController`), asynchronous event dispatcher (`ClientEventDispatcher`), and socket connection layer (`ServerConnection`). A Swing graphical interface is also provided (`LoginFrame`).
* **`src/test/`**: 90 automated unit and integration tests verifying business logic, concurrency, persistence, socket communications, and design patterns.

***

## Operating Instructions

All commands and scripts are executed from the project root directory.

### 1. Compile the System

Compiles all source files into the `out/` directory:

```bat
compile.bat
```

*Manual compilation command:*
```bash
mkdir out
javac -encoding UTF-8 -d out -sourcepath src src/server/core/ChainServer.java src/client/console/ConsoleMain.java
```

### 2. Start the Server

Start the server prior to launching clients. By default, the server listens on port **5000** (configured in `config.properties`):

```bat
run_server.bat
```

*Manual server startup command:*
```bash
java -cp out server.core.ChainServer
```

*Note:* On first startup, the server automatically initializes storage directories (`data/`, `logs/`, `reports/`) and populates initial demonstration employees and branch inventories (`DataSeeder`).

### 3. Start the Console Clients (Run in 2 Separate Windows)

To observe multi branch operations and inter branch communications, launch the client script in separate terminal windows:

```bat
run_client.bat
```

*Manual client startup command (run per window):*
```bash
java -cp out client.console.ConsoleMain
```

*(Optional graphical interface client: `run_client_gui.bat` or `java -cp out client.gui.LoginFrame`)*

***

## Default Demonstration Accounts

All demonstration accounts are provisioned with the initial password: **`Chain@2026`** (passwords are stored as SHA-256 hashes with individual salts).

| Employee No | Password | Full Name | Role | Branch | Privileges |
|---|---|---|---|---|---|
| **1001** | `Chain@2026` | Maya Shir | Shift Manager (Admin) | Tel Aviv | Full administrative access: Inventory, Customers, Chat, Employees, Password Policy, Reports (JSON/Word), Join active chats |
| **1002** | `Chain@2026` | Ron Levi | Cashier | Tel Aviv | Branch inventory, sales transactions, customer registration, branch chat |
| **2001** | `Chain@2026` | Avi Dagan | Shift Manager (Admin) | Jerusalem | Full administrative access for Jerusalem branch |
| **2002** | `Chain@2026` | Tamar Ben Ari | Seller | Jerusalem | Branch inventory, sales transactions, customer registration, branch chat |

***

## System Features and Functional Walkthrough

### 1. Client Server Authentication and Session Management
* Start the server, then launch a client.
* Enter employee number `1002` and password `Chain@2026`. The client connects over TCP sockets using serialized Java objects.
* Attempting to log in with an incorrect password or non existent user returns a structured error message. Concurrent duplicate logins for the same employee number are blocked.

### 2. Role Based User Interface
* **Cashier and Seller (e.g. 1002):** Displays core store operations (options 1 to 9: Inventory, Sales, Customers, Chat). Administrative operations remain restricted.
* **Shift Manager (e.g. 1001):** Displays administrative extensions (options 10 to 17: Add Products, Employee Management, Password Policy, Branch/Product Reports, Join In Progress Chats, View Audit Logs).

### 3. Branch Inventory Isolation and Stock Management
* **View Inventory (Option 1):** Displays products and stock quantities for the logged in branch. Tel Aviv and Jerusalem maintain isolated inventory repositories.
* **Restock Product (Option 3):** Simulates supplier inventory acquisition and increases available item quantities.

### 4. Customer Pricing Strategies and Dynamic Tier Upgrades
* **Customer Tiers:**
  * `NewCustomer`: 10% introductory discount applied to the first purchase.
  * `ReturningCustomer`: 5% permanent loyalty discount, or 8% discount when purchasing 3 or more items.
  * `VipCustomer`: 15% permanent discount, or 20% discount on transactions exceeding 500 NIS.
* **Polymorphic Pricing:** Sale calculations execute `customer.calculateFinalPrice(...)` without conditional type checking.
* **Automatic Tier Upgrades:** When thresholds are reached (1 purchase for Returning, 5 purchases or 1,000 NIS total spent for VIP), `CustomerFactory.upgradeIfNeeded()` instantiates the upgraded subclass while maintaining full transaction history.
* **Live Network Sync (Observer):** Registering or updating a customer from one terminal immediately emits a `CUSTOMERS_UPDATED` event to all connected terminals across the chain.

### 5. Sales Reporting and Export (JSON & Word RTF)
* Accessible to Shift Managers (`1001`).
* **Sales by Branch (Option 14):** Aggregates sales volume, items sold, gross revenue, and total discounts per branch.
* **Sales by Product (Option 15):** Generates product level sales breakdown.
* **JSON Export:** Emits structured JSON files into `reports/` and prints data directly to the terminal.
* **Word Export:** Emits standard Rich Text Format (`.rtf`) documents into `reports/`, opening directly in Microsoft Word with styled tables and without compatibility warnings.

### 6. Employee Management and Password Governance
* **Manage Employees (Options 11, 13):** Lists and creates employee records (Name, National ID, Phone, Bank Account, Branch, Role).
* **Password Policy (Option 12):** Shift managers can view and modify password complexity rules (minimum length, uppercase, lowercase, digits, special characters). New passwords are validated against active policy rules.

### 7. Inter Branch Live Chat and Monitor Queue
* **1 on 1 Inter Branch Chat (Options 7 & 8):** An employee in Tel Aviv requests a chat session with Jerusalem. An available Jerusalem employee receives a real time invitation.
* **Queue and Availability Callback:** If all employees in the target branch are occupied, the request is placed into a server managed queue. When an employee concludes a session, the server pushes a live `CHAT_PEER_AVAILABLE` notification to the requester.
* **Shift Manager Supervision (Option 16):** Shift managers can monitor active sessions, join in progress conversations, and review full message history.
* **Single Active Session Rule:** An employee can participate in at most one active chat session at a time.

### 8. System Activity and Audit Logs
* Shift managers can inspect real time activity logs via **Option 17**:
  * `logs/employees.log`: Employee creation and modifications.
  * `logs/customers.log`: Customer registrations and tier promotions.
  * `logs/sales.log`: Sales transactions and inventory restock operations.
  * `logs/chat.log`: Inter branch chat metadata (participants, branch, start time, duration).

***

## Design Patterns and Software Engineering Practices

| Design Pattern | Implementation Location | Engineering Value |
|---|---|---|
| **Observer** | `EventPublisher` (Server) / `ClientEventDispatcher`, `ServerEventListener` (Client) | Enables push updates (stock changes, customer promotions, chat invitations) without polling overhead. |
| **Strategy** | `Customer` hierarchy (`NewCustomer`, `ReturningCustomer`, `VipCustomer`) | Encapsulates distinct pricing rules within polymorphic subclasses. |
| **Factory** | `CustomerFactory`, `CommandFactory`, `ReportExporter` selection | Centralizes object instantiation and customer tier transitions based on business criteria. |
| **Command** | `server.command.Command` interface and 19 action handlers | Decouples network request routing from execution logic, centralizing authorization and audit logging. |
| **Singleton** | `SessionManager`, `LogManager`, `ClientRegistry`, `ServerContext`, `AppConfig`, `ChatService`, `ChatQueueManager` | Ensures synchronized central state coordination across concurrent threads. |
| **Monitor / Queue** | `ChatQueueManager` utilizing `wait()`, `notifyAll()`, and synchronized locks | Coordinates thread waiting when finding available chat peers, falling back to a managed queue. |

***

## Concurrency and Thread Safety

1. **Accept Loop:** `ChainServer` executes a non blocking `accept()` loop allocating an independent `ClientHandler` thread per connection.
2. **Dedicated Business Pool:** `BusinessTaskExecutor` bounds concurrent disk and persistence operations through `Executors.newFixedThreadPool(4)` (configurable via `business.threadPool.size`).
3. **Targeted Synchronization:** Fine grained object locking on specific `Product` instances during sales, and independent category locks in `LogWriter` to eliminate cross file contention.
4. **Thread Safe Data Structures:** Utilizes `ConcurrentHashMap` and `CopyOnWriteArrayList` for active sessions and event listeners.
5. **Graceful Termination:** Volatile boolean flags (`isRunning`, `isHandlerRunning`) ensure clean resource deallocation in `finally` blocks.
6. **Parallel Execution Demonstration:** Setting `demo.taskDelayMillis=1500` in `config.properties` provides visual confirmation of concurrent, interleaved tasks across worker threads.

***

## Automated Test Suites

To execute all **90 automated unit and integration tests**:

```bat
run_tests.bat
```

Tests run inside an isolated workspace (`test-workspace/`) to preserve demonstration data files.

***

## Storage Layout

System state is stored locally using Java Object Serialization and UTF-8 text logs:

```
data/
  employees.dat              # Serialized employee records and password hashes
  customers.dat              # Chain customer records
  sales.dat                  # Transaction history
  password_policy.dat        # System password validation rules
  inventory_TEL_AVIV.dat     # Tel Aviv branch stock
  inventory_JERUSALEM.dat    # Jerusalem branch stock
logs/
  employees.log, customers.log, sales.log, chat.log
reports/
  *.rtf, *.json              # Word RTF and JSON report exports
```

To reset the system to clean initial demonstration data, delete the `data/` and `logs/` directories. The server will reseed all initial data upon startup.
