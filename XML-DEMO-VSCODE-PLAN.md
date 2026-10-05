# VS Code Plan: Spring MVC + Hibernate (XML) Demo, linked to the Placement Portal

Goal: a small "classic" app that shows what Spring Boot hides (XML-configured Spring MVC, Hibernate
SessionFactory/Session, DB connection), running **side by side with the existing Boot app on the same
Supabase data**. The story for students: *same database, two eras of configuration.*

The scaffold already exists in `spring-hibernate-xml-demo.zip`. This plan rebuilds it in small steps so you
can live-code it, or you can unzip it and use the steps as a walkthrough.

---

## 0. Decisions (already made, so you do not debug them on stage)

| Topic | Decision | Why |
|---|---|---|
| Location | Sibling folder `xml-demo/` next to the Boot repo, opened in a **VS Code multi-root workspace** | Does not touch the Boot `pom.xml`; two terminals, two ports |
| Not a Maven module of the Boot repo | Correct | Boot parent POM would fight with Spring 6.1 + Hibernate 5.6 versions |
| Ports | Boot app 8080, XML app **8081** | Both run at the same time |
| Link | Both use the **same Supabase schema `app`, table `student`** | One real link, no code coupling |
| Safety | XML app uses `hbm2ddl.auto=validate`; Flyway in the Boot app stays the only schema owner | XML app can never alter your tables |
| Fallback | H2 in-memory mode (standalone, table `students`) | Works with no internet |

Two modes of the XML app:

| | Standalone (default) | Linked |
|---|---|---|
| DB | H2 in memory | Supabase, schema `app` |
| Table | `students` (name, email, course) | `app.student` (name, email, branch, cgpa) |
| `hbm2ddl.auto` | `update` | `validate` |

---

## 1. Prerequisites (VS Code)

Extensions: *Extension Pack for Java*, *Spring Boot Extension Pack*, *XML (Red Hat)*, *Community Server Connectors* (optional, only if you prefer a Tomcat server panel over Cargo).
Tools: JDK 17+ (21 is fine), Maven 3.9+, Git.

Checks:
```bash
java -version && mvn -v
echo $SUPABASE_DB_URL | cut -c1-40     # same variable the Boot app uses; never print the full string
```

Workspace: `File > Add Folder to Workspace` for both repos, then `File > Save Workspace As...` -> `ppsu.code-workspace`.

---

## 2. Build steps (one commit and tag per step, same habit as the Boot repo)

### Step 1: skeleton `[tag xml-step-1-skeleton]`
VS Code: create folder `xml-demo`, then the Maven structure.
```
xml-demo/
  pom.xml
  src/main/java/com/ppsu/xmldemo/{model,dao,service,controller}/
  src/main/resources/
  src/main/webapp/WEB-INF/views/
```
Copy `pom.xml` from the scaffold. Talking point: `<packaging>war</packaging>`, JSP and Servlet API are `provided`
(Tomcat supplies them), JSTL is not.
Verify: `mvn -q clean package` builds an empty `spring-hibernate-xml-demo.war`.

### Step 2: web.xml and the Front Controller `[tag xml-step-2-mvc]`
Files: `WEB-INF/web.xml`, `WEB-INF/dispatcher-servlet.xml`, `StudentController`, `views/students.jsp` (hard-code a fake list first).
Talking points: `ContextLoaderListener` = root context; `DispatcherServlet` = web context; ViewResolver turns `"students"` into `/WEB-INF/views/students.jsp`.
Verify: `mvn clean package cargo:run`, open `http://localhost:8081/spring-hibernate-xml-demo/students` (set the port in `pom.xml`, see section 4).

### Step 3: database connection `[tag xml-step-3-datasource]`
Files: `db.properties`, `applicationContext.xml` STEP 1 and 2 (`property-placeholder`, `HikariDataSource`).
Talking points: connection pool vs opening a connection per request; why credentials live outside Java code.
Verify: add a temporary `/db-check` endpoint, or just check the Hikari "Start completed" log line.

### Step 4: Hibernate SessionFactory `[tag xml-step-4-sessionfactory]`
Files: `hibernate.cfg.xml`, `Student.java` entity, `applicationContext.xml` STEP 3 (`LocalSessionFactoryBean`).
Talking points: **SessionFactory = one per app, heavy, thread-safe.** `show_sql` and `hbm2ddl` explained. Entity registered with `<mapping class=.../>`.
Verify: start the app, the console shows `create table students ...`.

### Step 5: Session, DAO, transactions `[tag xml-step-5-session-tx]`
Files: `StudentDao`, `StudentService`, `applicationContext.xml` STEP 4 and 5 (`HibernateTransactionManager`, `<tx:annotation-driven/>`, beans wired in XML).
Talking points: **Session = one unit of work.** `getCurrentSession()` only works inside a transaction. `@Transactional` opens and closes it.
Verify: add a student on the page, watch the `insert` SQL in the console. Remove `@Transactional` once to show the `No Session found for current thread` error, then put it back.

### Step 6: polish and README `[tag xml-step-6-final]`
`/db-check`, delete button, README with the "classic vs Boot" table.

---

## 3. Linking to the existing app

### 3a. Shared data (the real link)
Switch to **linked mode**:

1. `db.properties`: use the same env var as the Boot app, so no password is stored in the file.
   ```properties
   db.driver=org.postgresql.Driver
   db.url=${SUPABASE_DB_URL}
   ```
   Spring resolves `${SUPABASE_DB_URL}` from the environment. If that URL already carries the user and password, remove the `username` and `password` properties from the `HikariDataSource` bean; otherwise add `SUPABASE_DB_USER` / `SUPABASE_DB_PASSWORD` env vars and reference them.
2. `hibernate.cfg.xml`:
   ```xml
   <property name="hibernate.default_schema">app</property>
   <property name="hibernate.hbm2ddl.auto">validate</property>
   ```
3. `Student.java`: map the Boot app's table exactly.
   ```java
   @Table(name = "student")
   // fields: id, name, email, branch (String), cgpa (BigDecimal, column cgpa)
   ```
4. Controller, DAO and JSP: replace `course` with `branch` and add `cgpa` (form field + table column).
5. Seeded students have applications, so **deleting them fails on the foreign key**. Either hide the Delete button in linked mode or demo delete only on a student you just added. Say it out loud: it is a good foreign-key lesson.

Live proof (30 seconds): add "Demo Student" in the XML app at `:8081`, refresh `http://localhost:8080/students` in the Boot app, and the student appears. Then show the same row in the Supabase Table Editor.

### 3b. Navigation link (optional, 5 minutes)
In the Boot app's navigation template add: `<a href="http://localhost:8081/spring-hibernate-xml-demo/students">Classic XML version</a>`, and the reverse link in `students.jsp`. Only for the demo day; do not commit it to the main branch.

### 3c. Not recommended
REST calls between the two apps, shared libraries, or merging into one project. They add failure points and teach nothing about XML configuration.

---

## 4. Run configuration

`pom.xml`, Cargo plugin: change `<cargo.servlet.port>` from 8080 to **8081**. Also set a separate shutdown port so two Tomcats can coexist:
```xml
<cargo.rmi.port>8205</cargo.rmi.port>
<cargo.tomcat.ajp.port>8010</cargo.tomcat.ajp.port>
```
Optional `.vscode/tasks.json` so one click starts it:
```json
{
  "version": "2.0.0",
  "tasks": [{
    "label": "Run XML demo",
    "type": "shell",
    "command": "mvn clean package cargo:run",
    "options": { "cwd": "${workspaceFolder:xml-demo}" },
    "problemMatcher": []
  }]
}
```

---

## 5. Copy-paste prompts (if you build it with Claude Code or Copilot in VS Code)

**X1 Skeleton.** "Create a Maven WAR project `xml-demo`, Java 17, Spring Framework 6.1.14, Hibernate 5.6.15 Jakarta build (`hibernate-core-jakarta`), HikariCP, PostgreSQL and H2 drivers, Jakarta Servlet 6 and JSP provided, JSTL, Cargo plugin for Tomcat 10.1. No Spring Boot. Show the pom only."

**X2 MVC.** "Add `web.xml` (ContextLoaderListener + DispatcherServlet) and `dispatcher-servlet.xml` (component-scan, mvc:annotation-driven, InternalResourceViewResolver). One `StudentController` and a JSP list page. XML configuration only, no Java config classes."

**X3 DataSource.** "In `applicationContext.xml` add property-placeholder for `db.properties` and a HikariDataSource bean. Comment every step for students."

**X4 Hibernate.** "Add `hibernate.cfg.xml` (show_sql, format_sql, hbm2ddl, mapping class), a `Student` entity with jakarta.persistence, and a `LocalSessionFactoryBean` bean. Do not set a dialect."

**X5 Session and transactions.** "Add `StudentDao` using `sessionFactory.getCurrentSession()`, `StudentService` with `@Transactional`, `HibernateTransactionManager` and `tx:annotation-driven`. Wire DAO and service by XML property injection, not annotations."

**X6 Link.** "Switch the app to the existing Supabase schema `app`, table `student` (id, name, email, branch, cgpa), with `hbm2ddl.auto=validate` and `default_schema=app`. Read the URL from the `SUPABASE_DB_URL` environment variable. Update controller, DAO and JSP for `branch` and `cgpa`."

Rule for every prompt: *"Do not add anything I did not ask for. Keep it student-readable."*

---

## 6. Demo script (about 12 minutes, fits after the Spring MVC block)

| Min | Do | Say |
|---|---|---|
| 0-2 | Show the folder tree | "Before Boot, this is what every project looked like." |
| 2-4 | `web.xml`, then `dispatcher-servlet.xml` | "Two contexts. The browser only ever talks to DispatcherServlet." |
| 4-6 | `applicationContext.xml` STEP 2 | "A DataSource is a pool of ready connections." |
| 6-8 | `hibernate.cfg.xml` + SessionFactory bean | "One SessionFactory per app. Expensive. Built once." |
| 8-10 | `StudentDao` | "One Session per unit of work. Spring opens and closes it." |
| 10-12 | Add a student in the XML app, refresh the Boot app | "Same database. Boot just hides the XML." |

---

## 7. Gotchas checklist (run before the session)

- [ ] `mvn clean package cargo:run` has been run **once on the venue network** (Cargo downloads Tomcat the first time).
- [ ] Both apps start together (8080 and 8081, no port clash).
- [ ] `hbm2ddl.auto` is `validate` in linked mode. Never `update` against the shared schema.
- [ ] `cgpa` mapped as `BigDecimal`, `branch` present, or `validate` fails at startup (this error is a good teaching moment, but not live).
- [ ] No password committed; `.gitignore` covers `db.properties` changes if you put real values there.
- [ ] Fallback ready: `git checkout xml-step-N` for any step, and H2 standalone mode if Supabase is unreachable.
- [ ] Do not mix `javax.*` imports: everything is `jakarta.*`.
- [ ] Untested here: first run needs internet for Maven and Tomcat downloads.
