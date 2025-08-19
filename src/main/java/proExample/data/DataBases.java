package proExample.data;

import org.postgresql.ds.PGSimpleDataSource;
import proExample.config.ConfigDb;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
/**
 * Утилитарный класс для управления подключениями к БД и выполнения транзакций.
 * <p>
 * Особенности реализации:
 * <ul>
 *   <li>Поддерживает многопоточность — каждое соединение хранится в мапе {@code threadConnections}
 *       по ключу ID текущего потока.</li>
 *   <li>Поддерживает работу с несколькими базами данных одновременно (ключ — {@code jdbcUrl}).</li>
 *   <li>Использует ленивую инициализацию (через {@link Map#computeIfAbsent(Object, Function)}):
 *       соединения и источники данных создаются только тогда, когда они реально нужны.</li>
 *   <li>Предоставляет методы для работы с транзакциями (как с возвращаемым результатом, так и без).</li>
 * </ul>
 *
 * Пример использования:
 * <pre>{@code
 * String url = "jdbc:postgresql://localhost:5432/testdb";
 *
 * // Транзакция с возвратом значения
 * Integer count = DataBases.transaction(conn -> {
 *     try (Statement st = conn.createStatement();
 *          ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
 *         rs.next();
 *         return rs.getInt(1);
 *     } catch (SQLException e) {
 *         throw new RuntimeException(e);
 *     }
 * }, url);
 *
 * // Транзакция без возврата значения
 * DataBases.transaction(conn -> {
 *     try (PreparedStatement ps =
 *              conn.prepareStatement("INSERT INTO users(name) VALUES (?)")) {
 *         ps.setString(1, "test_user");
 *         ps.executeUpdate();
 *     } catch (SQLException e) {
 *         throw new RuntimeException(e);
 *     }
 * }, url);
 * }</pre>
 *
 * @author QA
 */
public class DataBases {
    /**
     * Кэш {@link DataSource} по JDBC-URL.
     * <p>
     * Нужен, чтобы не пересоздавать {@link PGSimpleDataSource}
     * при каждом подключении к одной и той же БД.
     */
    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();
    /**
     * Соединения, привязанные к потокам.
     * <p>
     * Вложенная структура:
     * <ul>
     *   <li>Ключ первого уровня — {@link Thread#getId()} (каждый поток хранит свои коннекшены).</li>
     *   <li>Ключ второго уровня — {@code jdbcUrl} (для поддержки нескольких БД).</li>
     *   <li>Значение — активное {@link Connection} для этой БД и потока.</li>
     * </ul>
     */
    private static final Map<Long, Map<String, Connection>> threadConnections = new ConcurrentHashMap<>();

    private DataBases() {
    }

    /**
     * Выполняет функцию в транзакции (с возвратом результата).
     * <p>
     * Алгоритм работы:
     * <ol>
     *   <li>Получаем соединение (создаётся новое, если ещё нет).</li>
     *   <li>Отключаем автокоммит.</li>
     *   <li>Вызываем функцию {@code function} с этим соединением.</li>
     *   <li>Если всё прошло успешно — делаем {@code commit()} и возвращаем результат.</li>
     *   <li>Если произошла ошибка — делаем {@code rollback()} и пробрасываем исключение.</li>
     * </ol>
     *
     * @param function функция, принимающая {@link Connection} и возвращающая результат
     * @param jdbcUrl  JDBC-URL базы данных
     * @param <T>      тип результата
     * @return результат выполнения функции
     */
    public static <T> T transaction(Function<Connection, T> function, String jdbcUrl) {
        Connection connection = null;
        try {
            connection = connection(jdbcUrl);
            connection.setAutoCommit(false);
            T result;
            result = function.apply(connection);
            connection.commit();
            connection.setAutoCommit(true);
            return result;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Выполняет функцию в транзакции (без возврата результата).
     * <p>
     * Алгоритм аналогичен {@link #transaction(Function, String)}, но используется {@link Consumer}.
     *
     * @param function функция, принимающая {@link Connection}
     * @param jdbcUrl  JDBC-URL базы данных
     */
    public static void transaction(Consumer<Connection> function, String jdbcUrl) {
        Connection connection = null;
        try {
            connection = connection(jdbcUrl);
            connection.setAutoCommit(false);
            function.accept(connection);
            connection.commit();
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    connection.setAutoCommit(true);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Закрывает все соединения во всех потоках и для всех баз данных.
     * <p>
     * Использовать, например, в {@code @AfterSuite}, чтобы корректно завершить работу.
     */
    public static void closeAllConnections() {
        for (Map<String, Connection> connectionMap : threadConnections.values()) {
            for (Connection connection : connectionMap.values()) {
                try {
                    if (connection != null && !connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException ignore) {
                }
            }

        }
    }

    /**
     * Получает соединение для текущего потока и конкретного JDBC-URL.
     * <p>
     * Реализация:
     * <ul>
     *   <li>Сначала ищем мапу соединений для текущего потока.</li>
     *   <li>Если нет — создаём новую мапу и кладём в неё первый коннекшен.</li>
     *   <li>Затем ищем соединение по конкретному URL в этой мапе.</li>
     *   <li>Если нет — создаём новое и сохраняем.</li>
     * </ul>
     *
     * @param jdbcUrl JDBC-URL базы данных
     * @return открытое {@link Connection}
     * @throws SQLException если не удалось создать соединение
     */
    private static Connection connection(String jdbcUrl) throws SQLException {
        return threadConnections.computeIfAbsent(
                Thread.currentThread().getId(),
                key -> {
                    try {
                        return new HashMap<>(Map.of(
                                jdbcUrl,
                                dataSource(jdbcUrl).getConnection()
                        ));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).computeIfAbsent(
                jdbcUrl,
                key -> {
                    try {
                        return dataSource(jdbcUrl).getConnection();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
    /**
     * Возвращает (или создаёт) {@link DataSource} для конкретного JDBC-URL.
     * <p>
     * Использует {@link PGSimpleDataSource} и настройки из {@link ConfigDb}.
     * При первом обращении создаётся новый {@code DataSource}, далее — берётся из кэша.
     *
     * @param jdbcUrl JDBC-URL базы данных
     * @return {@link DataSource} для подключения
     */
    private static DataSource dataSource(String jdbcUrl) {
        return dataSources.computeIfAbsent(
                jdbcUrl,
                key -> {
                    PGSimpleDataSource ds = new PGSimpleDataSource();
                    ds.setUser(ConfigDb.INSTANCE.jdbcUser());
                    ds.setPassword(ConfigDb.INSTANCE.jdbcPassword());
                    ds.setUrl(ConfigDb.INSTANCE.jdbcUrl());
                    return ds;
                }
        );
    }
}