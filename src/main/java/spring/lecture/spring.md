# Spring JDBC

## Введение

Spring JDBC — это модуль Spring Framework, который упрощает работу с базами данных и устраняет большинство проблем традиционного JDBC.

**Основные проблемы чистого JDBC:**
- Много boilerplate-кода (открытие/закрытие соединений, обработка исключений)
- Ручное управление ресурсами (Connection, Statement, ResultSet)
- Сложная обработка исключений SQLException
- Повторяющийся код для типичных операций

**Что решает Spring JDBC:**
- Автоматическое управление ресурсами
- Упрощенная обработка исключений
- Готовые шаблоны для типичных операций
- Интеграция с Spring-контейнером и транзакциями

## Архитектура Spring JDBC

```
Spring Application → JdbcTemplate → DataSource → Database Driver → TCP/IP → База данных
```

## Основные компоненты

### 1. JdbcTemplate

**Назначение:** Основной класс для выполнения SQL-операций в Spring JDBC.

**Ключевые особенности:**
- Thread-safe (можно использовать как singleton)
- Автоматически управляет ресурсами
- Конвертирует SQLException в DataAccessException
- Предоставляет множество методов для разных типов запросов

**Конфигурация:**
```
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        dataSource.setUsername("user");
        dataSource.setPassword("password");
        return dataSource;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 2. NamedParameterJdbcTemplate

**Назначение:** Расширение JdbcTemplate с поддержкой именованных параметров.

**Преимущества:**
- Более читаемые SQL-запросы
- Меньше ошибок при передаче параметров
- Поддержка Map и объектов как источников параметров

**Пример:**
```
@Service
public class UserService {
    
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    
    public UserService(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }
    
    public User findUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = :email";
        Map<String, Object> params = Map.of("email", email);
        
        return namedJdbcTemplate.queryForObject(sql, params, new UserRowMapper());
    }
}
```

### 3. RowMapper<T>

**Назначение:** Интерфейс для преобразования строк ResultSet в Java-объекты.

**Реализации:**
- **Кастомный RowMapper** — полный контроль над маппингом
- **BeanPropertyRowMapper** — автоматический маппинг по именам полей
- **Lambda-выражения** — для простых случаев

**Примеры:**

```
// Кастомный RowMapper
public class UserRowMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setAge(rs.getInt("age"));
        return user;
    }
}

// BeanPropertyRowMapper (автоматический)
RowMapper<User> rowMapper = new BeanPropertyRowMapper<>(User.class);

// Lambda-выражение
RowMapper<User> lambdaMapper = (rs, rowNum) -> {
    User user = new User();
    user.setId(rs.getLong("id"));
    user.setName(rs.getString("name"));
    return user;
};
```

### 4. SqlParameterSource

**Назначение:** Источник параметров для именованных запросов.

**Реализации:**
- **MapSqlParameterSource** — параметры из Map
- **BeanPropertySqlParameterSource** — параметры из свойств объекта

**Примеры:**
```
// MapSqlParameterSource
SqlParameterSource params = new MapSqlParameterSource()
    .addValue("name", "Иван")
    .addValue("email", "ivan@example.com")
    .addValue("age", 25);

// BeanPropertySqlParameterSource
User user = new User("Иван", "ivan@example.com", 25);
SqlParameterSource beanParams = new BeanPropertySqlParameterSource(user);
```

## Типичные операции

### SELECT операции

```
@Service
public class UserService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // Получение единственного объекта
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
    }
    
    // Получение списка объектов
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new UserRowMapper());
    }
    
    // Получение простого значения
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    
    // Получение Map
    public Map<String, Object> findUserAsMap(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.queryForMap(sql, id);
    }
}
```

### INSERT, UPDATE, DELETE операции

```
@Service
public class UserService {
    
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    
    // INSERT с возвратом сгенерированного ID
    public Long createUser(User user) {
        String sql = "INSERT INTO users (name, email, age) VALUES (:name, :email, :age)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new BeanPropertySqlParameterSource(user);
        
        namedJdbcTemplate.update(sql, params, keyHolder);
        return keyHolder.getKey().longValue();
    }
    
    // UPDATE
    public int updateUser(User user) {
        String sql = "UPDATE users SET name = :name, email = :email WHERE id = :id";
        SqlParameterSource params = new BeanPropertySqlParameterSource(user);
        
        return namedJdbcTemplate.update(sql, params);
    }
    
    // DELETE
    public int deleteUser(Long id) {
        String sql = "DELETE FROM users WHERE id = :id";
        Map<String, Object> params = Map.of("id", id);
        
        return namedJdbcTemplate.update(sql, params);
    }
    
    // Batch операции
    public int[] batchInsert(List<User> users) {
        String sql = "INSERT INTO users (name, email, age) VALUES (:name, :email, :age)";
        
        SqlParameterSource[] batchParams = users.stream()
            .map(BeanPropertySqlParameterSource::new)
            .toArray(SqlParameterSource[]::new);
            
        return namedJdbcTemplate.batchUpdate(sql, batchParams);
    }
}
```

## Обработка исключений

Spring JDBC автоматически конвертирует SQLException в иерархию DataAccessException:

**Основные типы исключений:**
- **DataIntegrityViolationException** — нарушение ограничений БД
- **EmptyResultDataAccessException** — queryForObject не нашел записей
- **IncorrectResultSizeDataAccessException** — найдено больше записей, чем ожидалось
- **DuplicateKeyException** — нарушение уникальности

**Обработка:**
```
@Service
public class UserService {
    
    public User findById(Long id) {
        try {
            String sql = "SELECT * FROM users WHERE id = ?";
            return jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            throw new UserNotFoundException("User with id " + id + " not found");
        }
    }
}
```

## Транзакции в Spring JDBC

### Декларативные транзакции (@Transactional)

```
@Service
@Transactional
public class BankService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // Транзакция на уровне метода
    @Transactional
    public void transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        // Списание
        String debitSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
        jdbcTemplate.update(debitSql, amount, fromAccountId);
        
        // Зачисление  
        String creditSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
        jdbcTemplate.update(creditSql, amount, toAccountId);
    }
    
    // Только для чтения
    @Transactional(readOnly = true)
    public List<Account> getAllAccounts() {
        String sql = "SELECT * FROM accounts";
        return jdbcTemplate.query(sql, new AccountRowMapper());
    }
}
```

### Программные транзакции

```
@Service
public class BankService {
    
    private final PlatformTransactionManager transactionManager;
    private final JdbcTemplate jdbcTemplate;
    
    public void transferMoneyProgrammatic(Long fromId, Long toId, BigDecimal amount) {
        TransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = transactionManager.getTransaction(def);
        
        try {
            // Операции с БД
            String debitSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";
            jdbcTemplate.update(debitSql, amount, fromId);
            
            String creditSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";
            jdbcTemplate.update(creditSql, amount, toId);
            
            transactionManager.commit(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

## Полная конфигурация приложения

### Application Properties
```
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=password
spring.datasource.driver-class-name=org.postgresql.Driver

# HikariCP Configuration
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

### Java Configuration
```
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
    
    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }
    
    @Bean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

## Сравнение: чистый JDBC vs Spring JDBC

### Чистый JDBC
```
public List<User> findAllUsers() {
    List<User> users = new ArrayList<>();
    String sql = "SELECT * FROM users";
    
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
        
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            users.add(user);
        }
    } catch (SQLException e) {
        throw new RuntimeException("Database error", e);
    }
    
    return users;
}
```

### Spring JDBC
```
public List<User> findAllUsers() {
    String sql = "SELECT * FROM users";
    return jdbcTemplate.query(sql, new UserRowMapper());
}
```

## Продвинутые возможности

### 1. SimpleJdbcInsert

**Назначение:** Упрощенные INSERT операции с автогенерацией SQL.

```
@Service
public class UserService {
    
    private final SimpleJdbcInsert insertUser;
    
    public UserService(DataSource dataSource) {
        this.insertUser = new SimpleJdbcInsert(dataSource)
            .withTableName("users")
            .usingGeneratedKeyColumns("id");
    }
    
    public Long createUser(User user) {
        Map<String, Object> params = new HashMap<>();
        params.put("name", user.getName());
        params.put("email", user.getEmail());
        params.put("age", user.getAge());
        
        Number key = insertUser.executeAndReturnKey(params);
        return key.longValue();
    }
}
```

### 2. SimpleJdbcCall

**Назначение:** Упрощенный вызов хранимых процедур.

```
@Service
public class ReportService {
    
    private final SimpleJdbcCall getUserReport;
    
    public ReportService(DataSource dataSource) {
        this.getUserReport = new SimpleJdbcCall(dataSource)
            .withProcedureName("get_user_report")
            .withoutProcedureColumnMetaDataAccess()
            .declareParameters(
                new SqlParameter("user_id", Types.BIGINT),
                new SqlOutParameter("report_data", Types.VARCHAR)
            );
    }
    
    public String generateReport(Long userId) {
        Map<String, Object> params = Map.of("user_id", userId);
        Map<String, Object> result = getUserReport.execute(params);
        return (String) result.get("report_data");
    }
}
```

### 3. Обработка больших ResultSet

```
@Service
public class DataProcessingService {
    
    private final JdbcTemplate jdbcTemplate;
    
    // ResultSetExtractor для обработки всего ResultSet
    public Map<String, Integer> getUserStatistics() {
        String sql = "SELECT department, COUNT(*) as count FROM users GROUP BY department";
        
        return jdbcTemplate.query(sql, rs -> {
            Map<String, Integer> stats = new HashMap<>();
            while (rs.next()) {
                stats.put(rs.getString("department"), rs.getInt("count"));
            }
            return stats;
        });
    }
    
    // RowCallbackHandler для потоковой обработки
    public void processAllUsers() {
        String sql = "SELECT * FROM users";
        
        jdbcTemplate.query(sql, rs -> {
            User user = new UserRowMapper().mapRow(rs, rs.getRow());
            // Обработка каждого пользователя
            processUser(user);
        });
    }
    
    private void processUser(User user) {
        // Логика обработки
        System.out.println("Processing user: " + user.getName());
    }
}
```

## Тестирование Spring JDBC

### Интеграционные тесты

```
@SpringBootTest
@Transactional
@Rollback
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    void shouldCreateAndFindUser() {
        // Given
        User user = new User("Тест", "test@example.com", 30);
        
        // When
        Long userId = userService.createUser(user);
        User foundUser = userService.findById(userId);
        
        // Then
        assertThat(foundUser.getName()).isEqualTo("Тест");
        assertThat(foundUser.getEmail()).isEqualTo("test@example.com");
    }
}
```

### Тесты с H2 in-memory базой

```
# application-test.properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop
```

```
@SpringBootTest
@ActiveProfiles("test")
@Sql("/data.sql") // Предварительная загрузка тестовых данных
class UserRepositoryTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void shouldFindUsersByAge() {
        List<User> users = userService.findUsersByAgeGreaterThan(18);
        assertThat(users).isNotEmpty();
    }
}
```

## Лучшие практики

### 1. Конфигурация и зависимости

```
<!-- Maven dependencies -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

### 2. Структура сервисного класса

```
@Service
@Transactional
public class UserService {
    
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final UserRowMapper userRowMapper;
    
    public UserService(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.userRowMapper = new UserRowMapper();
    }
    
    @Transactional(readOnly = true)
    public User findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = :id";
        Map<String, Object> params = Map.of("id", id);
        
        try {
            return namedJdbcTemplate.queryForObject(sql, params, userRowMapper);
        } catch (EmptyResultDataAccessException e) {
            return null; // или выбросить кастомное исключение
        }
    }
    
    public Long createUser(User user) {
        String sql = """
            INSERT INTO users (name, email, age, created_at) 
            VALUES (:name, :email, :age, :createdAt)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        SqlParameterSource params = new MapSqlParameterSource()
            .addValue("name", user.getName())
            .addValue("email", user.getEmail())
            .addValue("age", user.getAge())
            .addValue("createdAt", LocalDateTime.now());
        
        namedJdbcTemplate.update(sql, params, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
```

## Основные преимущества Spring JDBC

1. **Упрощение кода:** Убирает весь boilerplate-код
2. **Автоматическое управление ресурсами:** Не нужно вручную закрывать Connection/Statement/ResultSet
3. **Унифицированная обработка исключений:** SQLException → DataAccessException
4. **Интеграция с Spring:** Dependency Injection, транзакции, тестирование
5. **Гибкость:** Можно использовать как простые методы, так и продвинутые возможности
6. **Thread-safety:** JdbcTemplate можно использовать как singleton

## Когда использовать Spring JDBC

**Подходит для:**
- Проектов с простыми SQL-запросами
- Когда нужен полный контроль над SQL
- Высокопроизводительных приложений
- Легаси-систем с существующей схемой БД

**Не подходит для:**
- Сложных объектных моделей (лучше JPA/Hibernate)
- Когда нужно автоматическое создание схемы
- Приложений с частыми изменениями модели данных

Spring JDBC предоставляет золотую середину между простотой чистого JDBC и сложностью ORM-решений, обеспечивая высокую производительность и контроль при минимальном количестве boilerplate-кода.