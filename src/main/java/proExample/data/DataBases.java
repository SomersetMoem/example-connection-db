package proExample.data;

import proExample.config.ConfigDb;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataBases {
    // В случае многопоточных тестов и если будет несколько БД
    private static final Map<String, DataSource> dataSources = new ConcurrentHashMap<>();

    private DataBases() {
    }

    public static Connection connection(String jdbcUrl) throws SQLException {
        return dataSource(jdbcUrl).getConnection();
    }

    private static DataSource dataSource(String jdbcUrl) {
        //Тут надо рассписать как метод работает
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
