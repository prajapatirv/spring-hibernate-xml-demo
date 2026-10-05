# spring-hibernate-xml-demo: Spring MVC + Hibernate, XML configuration

Run: `mvn clean package cargo:run`, then open http://localhost:8081/spring-hibernate-xml-demo/students
(needs Maven 3.9+, JDK 17+, and internet on the first run).

Standalone mode uses H2 in memory. For linked mode (Supabase schema `app`), see XML-DEMO-VSCODE-PLAN.md section 3a.

| Classic (this app) | Spring Boot |
|---|---|
| web.xml + DispatcherServlet | auto-configured |
| HikariDataSource bean in XML | spring.datasource.* |
| LocalSessionFactoryBean | JPA auto-configuration |
| HibernateTransactionManager + tx:annotation-driven | transaction management auto-configured |
| WAR on Tomcat | embedded Tomcat, runnable JAR |
