package utils;

import java.sql.*;

public class DebugHelper {
    public static void main(String[] args) {
        System.out.println("🔍 Начинаем диагностику подключения...\n");

        // 1. Проверяем загрузку драйвера
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ PostgreSQL драйвер найден и загружен");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ PostgreSQL драйвер НЕ НАЙДЕН!");
            System.err.println("   Проверьте что в pom.xml есть зависимость:");
            System.err.println("   <dependency>");
            System.err.println("       <groupId>org.postgresql</groupId>");
            System.err.println("       <artifactId>postgresql</artifactId>");
            System.err.println("       <version>42.7.1</version>");
            System.err.println("   </dependency>");
            return;
        }

        // 2. Показываем все зарегистрированные драйверы
        System.out.println("\n🚗 Зарегистрированные JDBC драйверы:");
        java.util.Enumeration<Driver> drivers = DriverManager.getDrivers();
        boolean hasPostgresDriver = false;
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            String driverName = driver.getClass().getName();
            System.out.println("   - " + driverName);
            if (driverName.contains("postgresql")) {
                hasPostgresDriver = true;
                System.out.println("     ✅ PostgreSQL драйвер обнаружен!");
            }
        }

        if (!hasPostgresDriver) {
            System.err.println("❌ PostgreSQL драйвер не зарегистрирован в DriverManager!");
        }

        // 3. Проверяем разные варианты URL
        String[] testUrls = {
                "jdbc:postgresql://localhost:5432/testdb",
                "jdbc:postgresql://127.0.0.1:5432/testdb",
                "jdbc:postgresql://localhost:5432/postgres",
                "jdbc:postgresql://127.0.0.1:5432/postgres"
        };

        String username = "testuser";
        String password = "testpass";

        System.out.println("\n🔗 Тестируем подключения:");

        for (String url : testUrls) {
            System.out.println("\n   Тестируем: " + url);

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                System.out.println("   ✅ ПОДКЛЮЧЕНИЕ УСПЕШНО!");

                // Выполняем тестовый запрос
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT current_database(), current_user")) {

                    if (rs.next()) {
                        System.out.println("   📊 База данных: " + rs.getString(1));
                        System.out.println("   👤 Пользователь: " + rs.getString(2));
                    }
                }

                // Проверяем таблицы
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT tablename FROM pg_tables WHERE schemaname = 'public'")) {

                    System.out.println("   📋 Таблицы в базе:");
                    boolean hasTables = false;
                    while (rs.next()) {
                        System.out.println("      - " + rs.getString(1));
                        hasTables = true;
                    }
                    if (!hasTables) {
                        System.out.println("      (таблиц нет)");
                    }
                }

                return; // Если одно подключение успешно - выходим

            } catch (SQLException e) {
                System.err.println("   ❌ Ошибка: " + e.getMessage());
            }
        }

        System.err.println("\n❌ ВСЕ ПОПЫТКИ ПОДКЛЮЧЕНИЯ НЕУСПЕШНЫ!");

        // 4. Дополнительная диагностика сети
        System.out.println("\n🌐 Проверяем сетевую доступность:");

        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", 5432), 5000);
            System.out.println("   ✅ Порт 5432 на localhost доступен");
        } catch (Exception e) {
            System.err.println("   ❌ Порт 5432 на localhost недоступен: " + e.getMessage());
        }

        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("127.0.0.1", 5432), 5000);
            System.out.println("   ✅ Порт 5432 на 127.0.0.1 доступен");
        } catch (Exception e) {
            System.err.println("   ❌ Порт 5432 на 127.0.0.1 недоступен: " + e.getMessage());
        }

        // 5. Информация о Java и classpath
        System.out.println("\n☕ Информация о Java:");
        System.out.println("   Java версия: " + System.getProperty("java.version"));
        System.out.println("   Java home: " + System.getProperty("java.home"));

        String classpath = System.getProperty("java.class.path");
        System.out.println("\n📁 Classpath содержит PostgreSQL JAR:");
        boolean foundPostgresJar = classpath.toLowerCase().contains("postgresql");
        if (foundPostgresJar) {
            System.out.println("   ✅ PostgreSQL JAR найден в classpath");
        } else {
            System.out.println("   ❌ PostgreSQL JAR НЕ НАЙДЕН в classpath!");
            System.out.println("   Classpath: " + classpath);
        }
    }
}
