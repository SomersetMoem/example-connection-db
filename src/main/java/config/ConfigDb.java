package config;

import org.aeonbits.owner.Config;
import org.aeonbits.owner.ConfigFactory;

@Config.Sources("classpath:db.properties")
public interface ConfigDb extends Config {
    @Key("jdbc.url")
    String jdbcUrl();

    @Key("jdbc.user")
    String jdbcUser();

    @Key("jdbc.password")
    String jdbcPassword();

    ConfigDb INSTANCE = ConfigFactory.create(ConfigDb.class, System.getProperties());
}