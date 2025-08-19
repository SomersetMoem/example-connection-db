package jdbc.pro.service;

import jdbc.pro.config.ConfigDb;
import jdbc.pro.data.dao.impl.UserDaoJdbc;
import jdbc.pro.data.entity.user.UserEntity;
import jdbc.pro.model.UserJson;

import static jdbc.pro.data.DataBases.transaction;

public class UserDbClient {

    public UserJson createUser(UserJson userJson) {
        return transaction(connection -> {
            UserEntity user = UserEntity.fromJson(userJson);
            return UserJson.fromEntity(new UserDaoJdbc(connection).create(user));
        }, ConfigDb.INSTANCE.jdbcUrl());
    }
}
