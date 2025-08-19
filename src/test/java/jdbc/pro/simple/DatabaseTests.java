package jdbc.pro.simple;

import jdbc.simple.DatabaseHelper;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

public class DatabaseTests {
    private DatabaseHelper dbHelper;

    @BeforeEach
    public void setUp() {
        System.out.println("🚀 Инициализация подключения к БД");
        dbHelper = new DatabaseHelper();
    }

    @AfterEach
    public void tearDown() {
        if (dbHelper != null) {
            dbHelper.closeConnection();
        }
        System.out.println("🏁 Тест завершен\n");
    }

    @Test
    @DisplayName("Проверка подключения к базе данных")
    public void testDatabaseConnection() {
        System.out.println("🧪 Тест: Проверка подключения к БД");

        boolean isConnected = dbHelper.isConnectionValid();
        Assertions.assertTrue(isConnected, "Соединение с базой данных должно быть активным");

        System.out.println("✅ Подключение к БД работает корректно");
    }

    @Test
    @DisplayName("Проверка выполнения SELECT запроса")
    public void testSelectQuery() {
        System.out.println("🧪 Тест: Выполнение SELECT запроса");

        String query = "SELECT id, name, email FROM users ORDER BY id";
        List<Map<String, Object>> results = dbHelper.executeSelectQuery(query);

        Assertions.assertFalse(results.isEmpty(), "Результат запроса не должен быть пустым");

        Map<String, Object> firstUser = results.get(0);

        Assertions.assertTrue(firstUser.containsKey("id"), "Запись должна содержать поле 'id'");
        Assertions.assertTrue(firstUser.containsKey("name"), "Запись должна содержать поле 'name'");
        Assertions.assertTrue(firstUser.containsKey("email"), "Запись должна содержать поле 'email'");

        System.out.println("📋 Результаты запроса:");
        for (Map<String, Object> user : results) {
            System.out.println("   ID: " + user.get("id") +
                    ", Name: " + user.get("name") +
                    ", Email: " + user.get("email"));
        }

        System.out.println("✅ SELECT запрос выполнен успешно");
    }

    @Test
    @DisplayName("Проверка подсчета записей в таблице")
    public void testRecordCount() {
        System.out.println("🧪 Тест: Подсчет количества записей");

        int userCount = dbHelper.getRecordCount("users");
        Assertions.assertTrue(userCount > 0, "В таблице users должны быть записи");

        System.out.println("📊 Количество пользователей в БД: " + userCount);
        System.out.println("✅ Подсчет записей работает корректно");
    }
}