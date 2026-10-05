package com.ppsu.xmldemo.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Builds the pooled DataSource. Tries the primary (remote) database first;
 * if its URL is blank or it cannot be reached (no internet, bad credentials),
 * falls back to the in-memory H2 database so the app still starts.
 */
public class FallbackDataSourceFactory implements FactoryBean<DataSource>, DisposableBean {

    private String primaryDriver;
    private String primaryUrl;
    private String primaryUsername;
    private String primaryPassword;
    private String fallbackDriver;
    private String fallbackUrl;
    private String fallbackUsername;
    private String fallbackPassword;
    private int connectTimeoutSeconds = 3;

    private HikariDataSource dataSource;
    private boolean fallback;

    public void setPrimaryDriver(String v) { this.primaryDriver = v; }
    public void setPrimaryUrl(String v) { this.primaryUrl = v; }
    public void setPrimaryUsername(String v) { this.primaryUsername = v; }
    public void setPrimaryPassword(String v) { this.primaryPassword = v; }
    public void setFallbackDriver(String v) { this.fallbackDriver = v; }
    public void setFallbackUrl(String v) { this.fallbackUrl = v; }
    public void setFallbackUsername(String v) { this.fallbackUsername = v; }
    public void setFallbackPassword(String v) { this.fallbackPassword = v; }
    public void setConnectTimeoutSeconds(int v) { this.connectTimeoutSeconds = v; }

    /** True when the H2 fallback is in use. Read from XML via SpEL. */
    public boolean isFallback() {
        getObject();
        return fallback;
    }

    @Override
    public synchronized DataSource getObject() {
        if (dataSource == null) {
            if (isPrimaryReachable()) {
                dataSource = pool(primaryDriver, primaryUrl, primaryUsername, primaryPassword);
                fallback = false;
                System.out.println("[DB] Using primary database: " + primaryUrl);
            } else {
                dataSource = pool(fallbackDriver, fallbackUrl, fallbackUsername, fallbackPassword);
                fallback = true;
                System.out.println("[DB] Primary database unavailable, falling back to H2: " + fallbackUrl);
            }
        }
        return dataSource;
    }

    private boolean isPrimaryReachable() {
        if (primaryUrl == null || primaryUrl.isBlank()) {
            return false;
        }
        try {
            Class.forName(primaryDriver);
            DriverManager.setLoginTimeout(connectTimeoutSeconds);
            try (Connection c = DriverManager.getConnection(primaryUrl, primaryUsername, primaryPassword)) {
                return c.isValid(connectTimeoutSeconds);
            }
        } catch (Exception e) {
            System.out.println("[DB] Primary check failed: " + e.getMessage());
            return false;
        }
    }

    private HikariDataSource pool(String driver, String url, String user, String password) {
        HikariConfig cfg = new HikariConfig();
        cfg.setDriverClassName(driver);
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setMaximumPoolSize(5);
        return new HikariDataSource(cfg);
    }

    @Override
    public Class<?> getObjectType() { return DataSource.class; }

    @Override
    public boolean isSingleton() { return true; }

    @Override
    public void destroy() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
