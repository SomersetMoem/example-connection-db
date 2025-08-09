package utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper {
    private static final String URL = "jdbc:postgresql://localhost:5432/testdb";  // testdb!
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "testpass";

    private final Connection connection;

    public DatabaseHelper() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("✅ Подключение к базе данных установлено");
        } catch (SQLException e) {
            System.err.println("❌ Ошибка подключения к БД: " + e.getMessage());
            throw new RuntimeException("Не удалось подключиться к базе данных", e);
        }
    }

    /**
     * Выполнение SELECT запроса с возвратом результата
     */
    public List<Map<String, Object>> executeSelectQuery(String query) {
        List<Map<String, Object>> results = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }

            System.out.println("📊 Получено записей: " + results.size());

        } catch (SQLException e) {
            System.err.println("❌ Ошибка выполнения SELECT запроса: " + e.getMessage());
            throw new RuntimeException("Ошибка выполнения запроса", e);
        }

        return results;
    }

    /**
     * Проверка подключения к БД
     */
    public boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Закрытие соединения
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔒 Соединение с БД закрыто");
            } catch (SQLException e) {
                System.err.println("❌ Ошибка при закрытии соединения: " + e.getMessage());
            }
        }
    }

    /**
     * Получение одного значения из запроса
     */
    public Object getSingleValue(String query) {
        List<Map<String, Object>> results = executeSelectQuery(query);
        if (results.isEmpty()) {
            return null;
        }

        Map<String, Object> firstRow = results.get(0);
        return firstRow.values().iterator().next();
    }

    /**
     * Подсчет количества записей в таблице
     */
    public int getRecordCount(String tableName) {
        String query = "SELECT COUNT(*) FROM " + tableName;
        Object result = getSingleValue(query);
        return result != null ? ((Number) result).intValue() : 0;
    }
}