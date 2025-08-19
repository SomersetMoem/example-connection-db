package jdbc.pro.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jdbc.pro.data.entity.user.UserEntity;

import java.util.UUID;

public record UserJson(
        @JsonProperty("id") UUID id,
        @JsonProperty("name") String name,
        @JsonProperty("email") String email
) {
    public static UserJson fromEntity(UserEntity userEntity) {
        return new UserJson(
                userEntity.getId(),
                userEntity.getName(),
                userEntity.getEmail()
        );
    }
}