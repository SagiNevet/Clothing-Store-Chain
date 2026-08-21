# מערכת ניהול רשת חנויות בגדים

פרויקט גמר בקורס פיתוח אלגוריתמי JAVA, HIT, סמסטר קיץ 2026.
מרצה: רועי זימון

## חברי הקבוצה

| # | שם מלא | ת.ז |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
| 5 | | |

## הפעלה

צריך JDK מותקן. אין Maven ואין Gradle. נבדק על JDK 11.

```
javac -version
```

```
compile.bat
run_server.bat
run_client.bat
```

את `run_client.bat` מריצים פעמיים כדי לפתוח שני לקוחות, אחד לכל סניף.
בהרצה הראשונה נוצרות התיקיות `data`, `logs` ו-`reports`, ונוצרים עובדי דמו ומלאי התחלתי.

### משתמשי דמו

הסיסמה של כולם: `Chain@2026`

| מספר עובד | שם | תפקיד | סניף |
|---|---|---|---|
| 1001 | Maya Shir | מנהל משמרת | תל אביב |
| 1002 | Ron Levi | קופאי | תל אביב |
| 2001 | Avi Dagan | מנהל משמרת | ירושלים |
| 2002 | Tamar Ben Ari | מוכר | ירושלים |

מנהל משמרת רואה גם את הטאבים Employees ו-Reports, ויכול להצטרף לשיחת צ'אט שכבר פתוחה.

### בדיקות

```
run_tests.bat
```

הבדיקות משתמשות ב-`lib/junit-platform-console-standalone-1.10.2.jar`. שאר המערכת רצה בלי ספריות חיצוניות. התוצרים נכתבים ל-`test-workspace` ולא לקבצי ההרצה הרגילה.

### Javadoc

```
generate_javadoc.bat
```

הקבצים נוצרים ב-`javadoc/index.html`.

## סוגי לקוחות

הקריטריונים מוגדרים ב-`CustomerFactory` ואפשר לשנות אותם ב-`config.properties`.

| מעבר | תנאי |
|---|---|
| NewCustomer → ReturningCustomer | אחרי הרכישה הראשונה |
| ReturningCustomer → VipCustomer | אחרי 5 רכישות או 1000 ש"ח מצטבר, לפי מה שמגיע קודם |

| סוג | הנחה |
|---|---|
| NewCustomer | 10% על הרכישה הראשונה |
| ReturningCustomer | 5%, ו-8% בקנייה של 3 פריטים ומעלה |
| VipCustomer | 15%, ו-20% בהזמנה מעל 500 ש"ח |

כשלקוח עובר סוג נוצר אובייקט חדש במחלקה המתאימה (`CustomerFactory.upgradeIfNeeded`), כי סוג הלקוח מיוצג בירושה ולא בשדה.

## Design Patterns

| Pattern | מחלקות |
|---|---|
| Observer | `EventPublisher`, `ClientEventDispatcher`, `ServerEventListener` |
| Singleton | `SessionManager`, `LogManager`, `ClientRegistry`, `ServerContext`, `AppConfig`, `ChatService`, `ChatQueueManager`, `EventPublisher`, `ClientSession` |
| Strategy | `Customer`, `NewCustomer`, `ReturningCustomer`, `VipCustomer` |
| Factory | `CustomerFactory`, `CommandFactory`, ייצוא דוחות |
| Command | `Command` והמימושים ב-`server/command` |

## מבנה הפרויקט

```
src/
├── common/     model, protocol, exception, util
├── server/     core, command, service, observer, chat, report, storage
├── client/     net, controller, gui
└── test/
```

`common` משותף לשרת וללקוח ועובר בסריאליזציה.

## Threads

| מנגנון | מיקום |
|---|---|
| לולאת `accept` | `ChainServer` |
| thread לכל לקוח | `ClientHandler` (בלי הגבלת מספר) |
| `ExecutorService` | `BusinessTaskExecutor` |
| `wait` / `notify` | `ChatQueueManager` |
| `synchronized` | `InventoryService.sell`, `ConnectedClient.send`, `LogWriter`, `CustomerService` |
| Collections | `CopyOnWriteArrayList`, `ConcurrentHashMap` |
| עצירה עם דגל `volatile` | `ChainServer`, `ClientHandler`, `ServerConnection` |
| שחרור ב-`finally` | `ClientHandler` |
| האזנה בלקוח | `ServerConnection` |
| Swing EDT | `SwingUtilities.invokeLater` |

כדי לראות שתי פעולות במקביל בקונסולת השרת אפשר לשים ב-`config.properties`:

```
demo.taskDelayMillis=1500
```

ואז להריץ בו-זמנית פעולה אצל שני לקוחות.

## config.properties

```
server.port=5000
server.host=localhost
business.threadPool.size=4
demo.taskDelayMillis=0
chat.saveMessageContent=false
chat.waitForPartnerMillis=3000
customer.vip.minPurchases=5
customer.vip.minTotalSpent=1000
```

אם הקובץ נמחק המערכת עדיין עולה, כי לכל הגדרה יש ברירת מחדל בקוד.

## קבצים בזמן ריצה

```
data/      employees.dat, customers.dat, sales.dat, password_policy.dat,
           inventory_TEL_AVIV.dat, inventory_JERUSALEM.dat
logs/      employees.log, customers.log, sales.log, chat.log
reports/   דוחות .rtf ו-.json
```

אין מסד נתונים. האחסון הוא Java Object Serialization לקבצים.

## תקלות נפוצות

| בעיה | פתרון |
|---|---|
| `javac` לא מזוהה | ה-JDK לא ב-PATH |
| Could not connect to the server | השרת לא רץ. להריץ קודם `run_server.bat` |
| Address already in use | משהו כבר תופס את הפורט. לסגור אותו או לשנות `server.port` |
| רוצים נתונים נקיים | למחוק את `data` ו-`logs`. ההרצה הבאה תיצור אותם מחדש |
| שרת ולקוח על שני מחשבים | בצד הלקוח לשנות `server.host` לכתובת ה-IP של מחשב השרת |
