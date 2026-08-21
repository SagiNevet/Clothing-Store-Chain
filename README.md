# מערכת ניהול רשת חנויות בגדים

פרויקט גמר בקורס **פיתוח אלגוריתמי JAVA** — HIT, סמסטר קיץ 2026
מרצה: רועי זימון

---

## חברי הקבוצה

| # | שם מלא | ת.ז |
|---|---|---|
| 1 | _להשלים_ | _להשלים_ |
| 2 | _להשלים_ | _להשלים_ |
| 3 | _להשלים_ | _להשלים_ |
| 4 | _להשלים_ | _להשלים_ |
| 5 | _להשלים_ | _להשלים_ |

---

## הוראות הפעלה

### דרישה יחידה
**JDK מותקן.** זהו. אין Maven, אין Gradle, אין Spring, ואין צורך בשום IDE.
פותח ונבדק על **JDK 11 (Temurin 11.0.32)**.

בדיקה שה-JDK מותקן:
```
javac -version
```

### שלושה צעדים

```bat
compile.bat        REM 1. מקמפל את כל המערכת לתיקיית out
run_server.bat     REM 2. מפעיל את השרת — השאירו את החלון פתוח
run_client.bat     REM 3. מפעיל לקוח — הריצו אותו פעמיים, לשני סניפים
```

**הרצה ראשונה** יוצרת אוטומטית את התיקיות `data`, `logs` ו-`reports`,
ומזריעה 4 עובדי דמו ומלאי התחלתי לשני הסניפים. לא צריך להכין שום דבר מראש.

### משתמשי דמו

**הסיסמה זהה לכולם: `Chain@2026`**

| מספר עובד | שם | תפקיד | סניף |
|---|---|---|---|
| **1001** | Maya Shir | Shift Manager | Tel Aviv |
| 1002 | Ron Levi | Cashier | Tel Aviv |
| **2001** | Avi Dagan | Shift Manager | Jerusalem |
| 2002 | Tamar Ben Ari | Seller | Jerusalem |

> **מנהל משמרת הוא ה-admin של המערכת** — הוא היחיד שרואה את הטאבים
> Employees ו-Reports, והיחיד שיכול להצטרף לצ'אט קיים.

### הרצת הבדיקות (אופציונלי)
```bat
run_tests.bat
```
**90 בדיקות JUnit.** זהו המקום היחיד בפרויקט שמשתמש ב-jar חיצוני
(`lib/junit-platform-console-standalone-1.10.2.jar`), והוא כלי פיתוח בלבד —
**המערכת עצמה מתקמפלת ורצה בלי אף ספרייה חיצונית.**
הבדיקות כותבות לתיקייה נפרדת (`test-workspace`) ולא נוגעות בנתוני ההדגמה.

### יצירת ה-Javadoc (אופציונלי)
```bat
generate_javadoc.bat
```
נוצר ב-`javadoc/index.html`. **0 שגיאות, 0 אזהרות.**

---

## מה להדגים — מסלול מומלץ

הפעילו שרת ו-**שלושה** לקוחות: 1002 (קופאי ת"א), 1001 (מנהל ת"א), 2002 (מוכרת ירושלים).

| # | מה עושים | מה רואים |
|---|---|---|
| 1 | מתחברים כ-1002 ואז כ-1001 | לקופאי **3 טאבים**, למנהל **5** |
| 2 | מנסים להתחבר כ-1001 בחלון נוסף | נדחה: "already logged in from another computer" |
| 3 | ב-1002: Customers → Register customer | הלקוח מופיע **מיד** גם אצל 1001 וגם אצל 2002 |
| 4 | ב-1002: Inventory → Sell to customer | הכמות משתנה **מיד** אצל 1001 (אותו סניף), **לא** אצל 2002 |
| 5 | מוכרים לאותו לקוח 5 פעמים | עמודת Customer kind: New → Returning → **VIP**, וההנחה גדלה |
| 6 | ב-1001: Employees → Create account | חשבון נוצר; נסו סיסמה חלשה — נדחית עם כל ההפרות |
| 7 | ב-1002: Chat → Start conversation with Jerusalem | אצל 2002 **נפתחת שיחה מעצמה** |
| 8 | ב-1001: Chat → Join an open conversation | המנהל מקבל את **כל ההיסטוריה** |
| 9 | ב-1001: Reports → Build → Export to Word | נפתח ב-Word **בלי אזהרה** |
| 10 | פותחים את `logs/*.log` ב-Notepad | כל פעולה רשומה |

### להדגמת המקביליות (שלב 5)
לפני ההגנה שנו ב-`config.properties`:
```properties
demo.taskDelayMillis=1500
```
הריצו במקביל **רישום עובד** אצל 1001 ו**מכירה** אצל 1002, והצביעו על הקונסולה של השרת:
```
[Executor] START ADD_EMPLOYEE  on pool-1-thread-2
[Executor] START SELL_PRODUCT  on pool-1-thread-3   ← התחילה לפני שהראשונה נגמרה
[Executor] END   SELL_PRODUCT  on pool-1-thread-3 after 1545 ms
[Executor] END   ADD_EMPLOYEE  on pool-1-thread-2 after 1689 ms
```

---

## הקריטריון למעבר בין סוגי לקוחות

מוגדר ב-`CustomerFactory` וניתן לשינוי ב-`config.properties`:

| מעבר | תנאי |
|---|---|
| `NewCustomer` → `ReturningCustomer` | אחרי **הרכישה הראשונה** |
| `ReturningCustomer` → `VipCustomer` | אחרי **5 רכישות** *או* **1000 ש"ח מצטבר** (המוקדם מביניהם) |

### מסלולי הרכישה

| סוג לקוח | ההנחה |
|---|---|
| `NewCustomer` | 10% הנחת הצטרפות על הרכישה הראשונה בלבד |
| `ReturningCustomer` | 5% קבוע; **8%** בקנייה של 3 פריטים ומעלה |
| `VipCustomer` | 15% קבוע; **20%** בהזמנה מעל 500 ש"ח |

**המעבר מחליף את האובייקט ולא משנה שדה** — כי סוג הלקוח מיוצג ע"י המחלקה,
וזה מה שגורם לקריאה הפולימורפית לעבוד. `CustomerFactory.upgradeIfNeeded()`
יוצר מופע חדש ומעתיק את ההיסטוריה.

---

## היכן ממומש כל Design Pattern

| Pattern | מחלקות | מה הוא נותן |
|---|---|---|
| **Observer** | `server/observer/EventPublisher`<br>`client/net/ClientEventDispatcher`<br>`client/net/ServerEventListener` | השרת מודיע ללקוחות על שינוי בלי להכיר את ה-GUI. בלעדיו כל מסך היה עושה polling כל שנייה |
| **Singleton** | `SessionManager`, `LogManager`, `ClientRegistry`,<br>`ServerContext`, `AppConfig`, `ChatService`,<br>`ChatQueueManager`, `EventPublisher`, `ClientSession` | מצב גלובלי יחיד. שתי טבלאות sessions = מניעת התחברות כפולה נשברת |
| **Strategy** (בירושה) | `Customer` + `NewCustomer` / `ReturningCustomer` / `VipCustomer` | כל מסלול רכישה הוא אלגוריתם עצמאי. סוג לקוח חדש = מחלקה אחת, בלי לגעת בקוד המכירה |
| **Factory** | `CustomerFactory`, `CommandFactory`,<br>`ReportCommands.ExportReport.exporterFor` | יצירה לפי enum במקום `switch` שמתפזר בקוד |
| **Command** | `server/command/Command` + 19 מימושים | כל בקשה = אובייקט פעולה. הרשאות, לוגים וטיפול בשגיאות נכתבים **פעם אחת** סביב `command.execute()` |

---

## ארכיטקטורה

```
src/
├── common/          משותף לשרת וללקוח — עובר בסריאליזציה
│   ├── model/       19 מחלקות: Employee, Customer + 3 תתי-מחלקות, Product, Sale...
│   ├── protocol/    Request, Response, ServerEvent, ActionType, EventType, ProtocolKeys
│   ├── exception/   10 חריגות משלנו, כולן יורשות מ-ChainStoreException
│   └── util/        PasswordHasher, TimeUtil, AppConfig, IdGenerator
├── server/
│   ├── core/        ChainServer (accept loop), ClientHandler (thread לכל לקוח),
│   │                ConnectedClient, ClientRegistry, ServerContext, DataSeeder
│   ├── command/     Command + CommandFactory + 15 קבצי מימוש
│   ├── service/     Authentication, Session, Inventory, Customer, Employee,
│   │                Report, Log, BusinessTaskExecutor
│   ├── observer/    EventPublisher
│   ├── chat/        ChatService, ChatSession, ChatQueueManager, PendingChatRequest
│   ├── report/      ReportExporter + WordRtfExporter + JsonExporter
│   └── storage/     FileRepository גנרי + 5 repositories + LogWriter + StoragePaths
├── client/
│   ├── net/         ServerConnection (שליחה + האזנה אסינכרונית), ClientEventDispatcher
│   ├── controller/  7 controllers — המסכים לא יודעים מה זה Request
│   └── gui/         12 מחלקות Swing
└── test/            10 מחלקות, 90 בדיקות JUnit 5
```

**109 מחלקות מערכת + 10 מחלקות בדיקה.**

---

## Threads — היכן הם בקוד

| מנגנון | מיקום |
|---|---|
| `accept()` בלולאה | `ChainServer.acceptClientsUntilStopped` |
| thread לכל לקוח | `ChainServer` → `new Thread(new ClientHandler(socket))` — **ללא הגבלת מספר** |
| `ExecutorService` / `newFixedThreadPool` | `BusinessTaskExecutor` — תקרה על פעולות עסקיות מקבילות |
| `wait()` / `notify()` | `ChatQueueManager.findPartnerOrQueue` / `markFree` |
| `synchronized` על **בלוק** | `InventoryService.sell` (על המוצר), `ConnectedClient.send` (על הזרם), `LogWriter` (מנעול לכל קטגוריה), `CustomerService` (מנעול לכל לקוח) |
| Collections thread-safe | `CopyOnWriteArrayList`, `ConcurrentHashMap` |
| עצירה בדגל בוליאני | `ChainServer.isRunning`, `ClientHandler.isHandlerRunning`, `ServerConnection.isListening` — כולם `volatile`, **אף פעם לא `stop()`** |
| שחרור ב-`finally` | `ClientHandler.run` → `releaseConnection()` |
| thread האזנה בלקוח | `ServerConnection.listenForIncomingObjects` |
| Swing EDT | כל עדכון מסך דרך `SwingUtilities.invokeLater` |

---

## הגדרות — `config.properties`

```properties
server.port=5000                  # הפורט של השרת
server.host=localhost             # הכתובת שהלקוחות מתחברים אליה
business.threadPool.size=4        # כמה פעולות עסקיות במקביל
demo.taskDelayMillis=0            # 1500 לפני ההגנה, כדי לראות מקביליות בעין
chat.saveMessageContent=false     # האם לשמור את תוכן הודעות הצ'אט בלוג
chat.waitForPartnerMillis=3000    # כמה זמן בקשת צ'אט ממתינה לפני שהיא נכנסת לתור
customer.vip.minPurchases=5       # סף VIP לפי מספר רכישות
customer.vip.minTotalSpent=1000   # סף VIP לפי סכום מצטבר
```

המערכת עולה תקין גם אם הקובץ נמחק — לכל הגדרה יש ברירת מחדל בקוד.

---

## קבצים שנוצרים בזמן ריצה

```
data/     employees.dat, customers.dat, sales.dat, password_policy.dat,
          inventory_TEL_AVIV.dat, inventory_JERUSALEM.dat
logs/     employees.log, customers.log, sales.log, chat.log   (טקסט — נפתח ב-Notepad)
reports/  sales_by_branch_<timestamp>.rtf, ...                (נפתח ב-Word)
```

**אין מסד נתונים.** הכל בקבצים, עם Java Object Serialization.

---

## תיעוד נוסף

בתיקיית `docs/` יש הסבר מפורט לכל שלב בעברית, כולל **שאלות הגנה צפויות עם תשובות**:

| קובץ | נושא |
|---|---|
| [`DEFENSE_CHEATSHEET.md`](docs/DEFENSE_CHEATSHEET.md) | **30 השאלות הסבירות ביותר עם תשובות — התחילו מכאן** |
| [`STAGE1_EXPLAINED.md`](docs/STAGE1_EXPLAINED.md) | מודל, פרוטוקול, חריגות, אחסון, פולימורפיזם |
| [`STAGE2_EXPLAINED.md`](docs/STAGE2_EXPLAINED.md) | שרת, thread לכל לקוח, מניעת התחברות כפולה |
| [`STAGE3_EXPLAINED.md`](docs/STAGE3_EXPLAINED.md) | GUI, שליחה והאזנה בו-זמנית, כללי Swing |
| [`STAGE4_EXPLAINED.md`](docs/STAGE4_EXPLAINED.md) | מלאי, לקוחות, Observer, race conditions |
| [`STAGE5_EXPLAINED.md`](docs/STAGE5_EXPLAINED.md) | ExecutorService והדגמת המקביליות |
| [`STAGE6_EXPLAINED.md`](docs/STAGE6_EXPLAINED.md) | צ'אט, תור, `wait`/`notify` |
| [`STAGE7_EXPLAINED.md`](docs/STAGE7_EXPLAINED.md) | דוחות וייצוא ל-Word |
| [`PLAN_STAGE0.md`](PLAN_STAGE0.md) | התכנון המקורי |

---

## פתרון תקלות

| בעיה | פתרון |
|---|---|
| `javac` לא מזוהה | ה-JDK לא ב-PATH. התקינו JDK והוסיפו את `bin` שלו ל-PATH |
| הלקוח: "Could not connect to the server" | השרת לא רץ. הריצו `run_server.bat` קודם |
| "Address already in use" | שרת אחר כבר תופס את הפורט. סגרו אותו או שנו `server.port` |
| רוצים להתחיל מנתונים נקיים | מחקו את התיקיות `data` ו-`logs`. ההרצה הבאה תזריע מחדש |
| רוצים להריץ שרת ולקוח על שני מחשבים | שנו `server.host` בקובץ של הלקוח לכתובת ה-IP של מחשב השרת |
