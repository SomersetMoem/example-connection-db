package pro.service;

import proExample.config.ConfigDb;
import proExample.data.dao.impl.UserDaoJdbc;
import proExample.data.entity.user.UserEntity;
import proExample.model.UserJson;

import static proExample.data.DataBases.transaction;

public class UserDbClient {

    public UserJson createUser(UserJson userJson) {
        return transaction(connection -> {
            UserEntity user = UserEntity.fromJson(userJson);
            return UserJson.fromEntity(new UserDaoJdbc(connection).create(user));
        }, ConfigDb.INSTANCE.jdbcUrl());
    }
}
