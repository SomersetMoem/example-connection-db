package jdbc.pro.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import jdbc.pro.service.UserDbClient;
import jdbc.pro.extensions.DataBasesExtension;
import jdbc.pro.model.UserJson;

@ExtendWith(DataBasesExtension.class)
public class JdbcTest {

    @Test
    public void userCreateTest() {
        UserDbClient userDbClient = new UserDbClient();
        UserJson userJson = userDbClient.createUser(new UserJson(null, "Vasy", "asS@SASD.ru"));
        Assertions.assertNotNull(userJson);
    }
}