package pro.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import pro.service.UserDbClient;
import proExample.extensions.DataBasesExtension;
import proExample.model.UserJson;

@ExtendWith(DataBasesExtension.class)
public class JdbcTest {

    @Test
    public void userCreateTest() {
        UserDbClient userDbClient = new UserDbClient();
        UserJson userJson = userDbClient.createUser(new UserJson(null, "Vasy", "asS@SASD.ru"));
        Assertions.assertNotNull(userJson);
    }
}