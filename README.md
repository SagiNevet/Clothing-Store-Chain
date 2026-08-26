# Clothing Store Chain

A Java/Swing client-server system for managing a clothing store chain, written for the Java course at HIT.

## Team

| Name | ID |
|---|---|
| Teva Jalink | 208181446 |
| [FILL IN] | [FILL IN] |
| [FILL IN] | [FILL IN] |
| [FILL IN] | [FILL IN] |
| [FILL IN] | [FILL IN] |

## Requirements

- Tested on JDK 11.
- No Maven, no Gradle, no Spring, no external libraries. The system is pure JDK.
- JUnit is used for tests only (`lib/junit-platform-console-standalone-1.10.2.jar`). The application itself compiles and runs with no external library.

## How to run

Run everything from the project root. The `.bat` scripts are the recommended way on Windows. The `javac` / `java` commands are a backup if the scripts do not run.

### A. Compile

Compiles `src/common`, `src/server` and `src/client` into `out`. Tests are not compiled here.

Recommended:

```
compile.bat
```

Manual backup:

```
mkdir out
javac -encoding UTF-8 -d out -sourcepath src src/server/core/ChainServer.java src/client/gui/LoginFrame.java
```

### B. Run the server

Start the server **before** any client. It listens on port **5000** (`server.port` in `config.properties`, default 5000 in `ChainServer`).

Recommended:

```
run_server.bat
```

Manual backup:

```
java -cp out server.core.ChainServer
```

Leave this window open.

The first run creates `data`, `logs` and `reports` if they are missing, and seeds demo employees plus starting inventory when those files are still empty (`DataSeeder`).

### C. Run a client

Run a client **twice, in two separate windows**, so you can log in as Tel Aviv in one and Jerusalem in the other. That is how chat and live updates are checked.

Recommended (run this twice):

```
run_client.bat
```

Each call opens a new window.

Manual backup (run once in each of two terminals):

```
java -cp out client.gui.LoginFrame
```

Host and port come from `config.properties` (`server.host=localhost`, `server.port=5000`).

## Demo users

Log in with the **employee number** and password. All demo accounts are created by `DataSeeder` with the same password `Chain@2026` (stored hashed). There is no separate Admin role: a Shift Manager is the admin of the system.

| Username | Password | Role | Branch |
|---|---|---|---|
| 1001 | Chain@2026 | Shift Manager | Tel Aviv |
| 1002 | Chain@2026 | Cashier | Tel Aviv |
| 2001 | Chain@2026 | Shift Manager | Jerusalem |
| 2002 | Chain@2026 | Seller | Jerusalem |

1001 is the Tel Aviv admin. 2002 is a Jerusalem seller. Cashier and Seller have the same permissions; Shift Manager also sees Employees and Reports, and can join an open chat.

## What to test

1. **Login by role.** Log in as 1002 (Cashier): Inventory, Customers, Chat. Log in as 1001 (Shift Manager): the same tabs plus Employees and Reports.
2. **Sale in one branch (Observer).** Sell a product in Tel Aviv. Stock updates at once for every client of Tel Aviv, not for Jerusalem.
3. **Customer add/update.** Register or edit a customer. The change appears in every connected branch (the customer list is shared).
4. **Customer kinds (Strategy).** New: 10% on the first purchase. Returning: 5%, or 8% on 3+ items. VIP: 15%, or 20% on orders above 500. The price comes from the subclass, not from an if on the type.
5. **Chat (queue).** Open a conversation from Tel Aviv to Jerusalem while someone is free: the other window opens the chat. Then start a chat while the Jerusalem employee is already in a conversation: the request is kept in the queue. When that employee becomes free, the waiting side gets a notification (`CHAT_PEER_AVAILABLE`) and can try again. `ChatQueueManager` waits with `wait()` / `notifyAll()` for a short window (`chat.waitForPartnerMillis`, default 3000 ms); after that the request stays queued instead of holding a pool thread.
6. **Join chat.** Only a Shift Manager can join an open conversation (Chat tab, join). A Cashier or Seller cannot.
7. **Duplicate login.** Log in as 1001, then try 1001 again in another window. The second login is rejected.
8. **Threads.** Create an employee as 1001 and sell as 1002 at the same time. Neither action blocks the other. Optional: set `demo.taskDelayMillis=1500` in `config.properties` so overlapping `START` / `END` lines are easy to see on the server console.
9. **Reports.** As a Shift Manager, build a report and export to Word (RTF file under `reports/`).
10. **Logs.** Open `logs/employees.log`, `logs/customers.log`, `logs/sales.log`, `logs/chat.log`. Each action type has its own file.

## Project structure

```
src/
  common/     Shared model, protocol, exceptions, utilities (serialized over the socket)
  server/     Accept loop, commands, services, chat, reports, file storage
  client/     Swing GUI, controllers, connection
  test/       JUnit tests (compiled only by run_tests.bat)
```

## Design patterns and key decisions

| Pattern | Where |
|---|---|
| Observer | `EventPublisher` on the server, `ClientEventDispatcher` / `ServerEventListener` on the client. Inventory events go to one branch. Customer events go to every client. |
| Singleton | `SessionManager`, `LogManager`, `ClientRegistry`, `ServerContext`, `AppConfig`, `ChatService`, `ChatQueueManager`, `EventPublisher`, `ClientSession` |
| Strategy | `Customer` with `NewCustomer`, `ReturningCustomer`, `VipCustomer` (each kind has its own price calculation) |
| Factory | `CustomerFactory`, `CommandFactory`, report exporter selection |
| Command | `server.command.Command` and the action classes under `server/command/impl` |
| Chat queue | Monitor: `wait()` / `notifyAll()` plus a FIFO waiting list in `ChatQueueManager` |

Customer kind changes (`CustomerFactory`):

- New becomes Returning after the first completed purchase.
- Returning becomes VIP after 5 purchases **or** 1000 total spent, whichever comes first (`customer.vip.minPurchases` and `customer.vip.minTotalSpent` in `config.properties`).
- The object is replaced with a new instance of the next class. The kind is the class, not a field.

Word export:

- Reports are written as **RTF** (`.rtf`) by `WordRtfExporter`, using only JDK file writing.
- No Apache POI or other library. HTML saved as `.doc` was not used, because Word shows a format warning. Word opens RTF without that warning.

## Data files

Created at runtime under the folder the server was started from (usually the project root). There is no database. State is Java object serialization. Logs are plain text.

```
data/      employees.dat, customers.dat, sales.dat, password_policy.dat,
           inventory_TEL_AVIV.dat, inventory_JERUSALEM.dat
logs/      employees.log, customers.log, sales.log, chat.log
reports/   sales_by_branch_<timestamp>.rtf (and .json), same for sales_by_product
```

To start clean, delete `data` and `logs`. The next server start seeds the demo employees and inventory again.
