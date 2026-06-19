package com.lapisdev.database;

import java.sql.Connection;
import java.sql.ResultSet;

public class DatabaseConnection {
    private Connection con;

    public DatabaseConnection(String connectionString) {
        try {
            con = java.sql.DriverManager.getConnection(connectionString);
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to the database: " + e.getMessage(), e);
        }
    }

    public void close() {
        try {
            if (con != null) con.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to close the database connection: " + e.getMessage(), e);
        }
    }

    public void createTable(String table) {
        try {
            con.createStatement().execute("CREATE TABLE IF NOT EXISTS " + table);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create table '" + table + "': " + e.getMessage(), e);
        }
    }

    public ResultSet query(final String sql, Object... params) {
        try {
            var stmt = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeQuery();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute query'" + sql + "': " + e.getMessage(), e);
        }
    }

    public ResultSet query(final String sql) {
        return query(sql, new Object[0]);
    }

    public int update(final String sql, Object... params) {
        try {
            var stmt = con.prepareStatement(sql);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            return stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute update'" + sql + "': " + e.getMessage(), e);
        }
    }

    public int update(final String sql) {
        return update(sql, new Object[0]);
    }

    public int insert(final String sql, Object... params) {
        try {
            var stmt = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                throw new RuntimeException("Failed to retrieve generated key for insert'" + sql + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute insert'" + sql + "': " + e.getMessage(), e);
        }
    }

    public int insert(final String sql) {
        return insert(sql, new Object[0]);
    }
}
