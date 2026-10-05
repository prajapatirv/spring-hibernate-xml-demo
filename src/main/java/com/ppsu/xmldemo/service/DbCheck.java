package com.ppsu.xmldemo.service;

import javax.sql.DataSource;
import java.sql.Connection;

public class DbCheck {
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) { this.dataSource = dataSource; }

    public String check() {
        try (Connection c = dataSource.getConnection()) {
            return "OK: " + c.getMetaData().getDatabaseProductName() + " " + c.getMetaData().getDatabaseProductVersion();
        } catch (Exception e) {
            return "FAILED: " + e.getMessage();
        }
    }
}
