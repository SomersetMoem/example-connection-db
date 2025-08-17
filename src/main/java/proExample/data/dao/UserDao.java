package proExample.data.dao;

import proExample.data.entity.user.UserEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserDao {
    UserEntity create(UserEntity userEntity);

    Optional<UserEntity> findUser(UUID id);
}
