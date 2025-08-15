package data;

import config.ConfigDb;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataBases {
    private DataBases() {
    }

    // В случае многопоточных тестов и если будет несколько БД
    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    private static DataSource dataSource(String jdbcUrl) {
        //Тут надо рассписать как метод работает
        return dataSources.computeIfAbsent(
                jdbcUrl,
                key -> {
                    PGSimpleDataSource ds = new PGSimpleDataSource();
                    ds.setUser(ConfigDb.INSTANCE.jdbcUser());
                    ds.setPassword(ConfigDb.INSTANCE.jdbcPassword());
                }
        )
    }

}
