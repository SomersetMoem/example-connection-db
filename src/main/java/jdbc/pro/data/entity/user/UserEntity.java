package jdbc.pro.data.entity.user;


import lombok.Getter;
import lombok.Setter;
import jdbc.pro.model.UserJson;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
public class UserEntity implements Serializable {
    private UUID id;
    private String name;
    private String email;

    public static UserEntity fromJson(UserJson userJson) {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userJson.id());
        userEntity.setName(userJson.name());
        userEntity.setEmail(userJson.email());
        return userEntity;
    }
}
