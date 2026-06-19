package com.lapisdev.database;

import java.sql.ResultSet;
import java.util.ArrayList;

public interface DatabaseTable<T extends DatabaseTable<T>> {
    default String tableName() {
        try {
            return StringUtils.toSnakeCase(this.getClass().getSimpleName());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get table name for class " + this.getClass().getName(), e);
        }
    }

    default String tableDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append(tableName()).append(" (");
        for (var field : this.getClass().getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                sb.append(field.getName()).append(" ").append(getSQLType(field.getType(), field.getName())).append(", ");
            }
        }
        sb.setLength(sb.length() - 2);
        sb.append(")");
        return sb.toString();
    }

    default void createTable() {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            con.createTable(tableDefinition());
        }
    }

    default String getSQLType(Class<?> type, String name) {
        if (name.equalsIgnoreCase("id")) {
            return "INTEGER PRIMARY KEY AUTOINCREMENT";
        }
        if (type == int.class || type == Integer.class) {
            return "INTEGER";
        } else if (type == long.class || type == Long.class) {
            return "BIGINT";
        } else if (type == float.class || type == Float.class) {
            return "REAL";
        } else if (type == double.class || type == Double.class) {
            return "DOUBLE";
        } else if (type == boolean.class || type == Boolean.class) {
            return "BOOLEAN";
        } else if (type == String.class) {
            return "TEXT";
        } else {
            throw new RuntimeException("Unsupported field type: " + type.getName());
        }
    }

    default ArrayList<String> getFieldNames() {
        ArrayList<String> names = new ArrayList<>();
        for (var field : this.getClass().getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                names.add(field.getName());
            }
        }
        return names;
    }

    default ArrayList<String> getFieldNamesExceptId() {
        ArrayList<String> names = getFieldNames();
        names.removeIf(name -> name.equalsIgnoreCase("id"));
        return names;
    }

    default ArrayList<Object> getFieldValues() {
        ArrayList<Object> values = new ArrayList<>();
        for (var field : this.getClass().getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                try {
                    values.add(field.get(this));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field " + field.getName() + " of class " + this.getClass().getName(), e);
                }
            }
        }
        return values;
    }

    default ArrayList<Object> getFieldValuesExceptId() {
        ArrayList<Object> values = new ArrayList<>();
        for (var field : this.getClass().getDeclaredFields()) {
            if (!java.lang.reflect.Modifier.isStatic(field.getModifiers()) && !field.getName().equalsIgnoreCase("id")) {
                field.setAccessible(true);
                try {
                    values.add(field.get(this));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Failed to access field " + field.getName() + " of class " + this.getClass().getName(), e);
                }
            }
        }
        return values;
    }

    default T fromResultSet(java.sql.ResultSet rs) {
        try {
            for (var field : this.getClass().getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    field.set(this, rs.getObject(field.getName()));
                }
            }
            return (T)this;
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate table model for class " + this.getClass().getName(), e);
        }
    }

    default int getId() {
        try {
            var field = this.getClass().getDeclaredField("id");
            field.setAccessible(true);
            return field.getInt(this);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to get ID: does the class have an `id` field? " + e, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access ID field: " + e, e);
        }
    }

    default void setId(int id) {
        try {
            var field = this.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.setInt(this, id);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Failed to set ID: does the class have an `id` field? " + e, e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access ID field: " + e, e);
        }
    }

    default ArrayList<T> select(String whereClause, Object... params) {
        String sql;
        if (whereClause == null || whereClause.isEmpty()) {
            sql = "SELECT * FROM " + tableName();
        } else {
            sql = "SELECT * FROM " + tableName() + " WHERE " + whereClause;
        }
        return query(sql, params);
    }

    default ArrayList<T> select(String whereClause) {
        return select(whereClause, new Object[0]);
    }

    default ArrayList<T> select() {
        return select(null, new Object[0]);
    }

    default int insert() {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            String csvFieldNames = String.join(", ", getFieldNamesExceptId());
            String csvFieldPlaceholders = String.join(", ", "?".repeat(getFieldValuesExceptId().size()).split(""));
            String sql = "INSERT INTO " + tableName() + " (" + csvFieldNames + ") VALUES (" + csvFieldPlaceholders + ")";
            int id = con.insert(sql, getFieldValuesExceptId().toArray());
            setId(id);
            return id;
        }
    }

    default int update(String whereClause, Object... params) {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            String sql = "UPDATE " + tableName() + " SET " + String.join(", ", getFieldValues().stream().map(_ -> "?").toArray(String[]::new)) + " WHERE " + whereClause;
            Object[] allParams = new Object[getFieldValues().size() + params.length];
            System.arraycopy(getFieldValues().toArray(), 0, allParams, 0, getFieldValues().size());
            System.arraycopy(params, 0, allParams, getFieldValues().size(), params.length);
            return con.update(sql, allParams);
        }
    }

    default int update(String whereClause) {
        return update(whereClause, getFieldValues());
    }

    default int update() {
        return update("id = ?", getId());
    }

    default int delete(String whereClause, Object... params) {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            String sql = "DELETE FROM " + tableName() + " WHERE " + whereClause;
            return con.update(sql, params);
        }
    }

    default int delete() {
        return delete("id = ?", getId());
    }

    default int deleteAll() {
        return execute("DELETE FROM " + tableName());
    }

    default ArrayList<T> query(String fullSql, Object... params) {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            ResultSet rs = con.query(fullSql, params);
            ArrayList<T> results = new ArrayList<>();
            try {
                while (rs.next()) {
                    results.add(fromResultSet(rs));
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate table model for class " + getClass().getSimpleName(), e);
            }
            return results;
        }
    }

    default ArrayList<T> query(String fullSql) {
        return query(fullSql, new Object[0]);
    }

    default int execute(String fullSql, Object... params) {
        try (DatabaseConnection con = DatabaseConnectionManager.current.open()) {
            return con.update(fullSql, params);
        }
    }

    default int execute(String fullSql) {
        return execute(fullSql, new Object[0]);
    }

    default ArrayList<T> from(String fieldName, Object value) {
        return select(fieldName + " = ?", value);
    }

    default T fromId(int id) {
        return QueryArray.single(select("id = ?", id));
    }
}
