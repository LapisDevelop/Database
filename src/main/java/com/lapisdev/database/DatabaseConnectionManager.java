package com.lapisdev.database;

public class DatabaseConnectionManager {
    public static DatabaseConnectionManager current;

    public String connectionString;

    public DatabaseConnectionManager(String connectionString) {
        DatabaseConnectionManager.current = this;
        this.connectionString = connectionString;
    }

    public DatabaseConnection open() {
        return new DatabaseConnection(connectionString);
    }
}
