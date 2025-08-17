package proExample.data.dao.impl;

import proExample.config.ConfigDb;
import proExample.data.DataBases;
import proExample.data.dao.UserDao;
import proExample.data.entity.user.UserEntity;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserDaoJdbc implements UserDao {
    @Override
    public UserEntity create(UserEntity userEntity) {
        try (Connection connection = DataBases.connection(ConfigDb.INSTANCE.jdbcUrl())) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO user (id, username, email) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            )) {
                preparedStatement.setObject(1, userEntity.getId());
                preparedStatement.setString(2, userEntity.getName());
                preparedStatement.setString(3, userEntity.getEmail());

                preparedStatement.executeUpdate();

                final UUID id;
                try (ResultSet rs = preparedStatement.getResultSet()) {
                    if (rs.next()) {
                        id = rs.getObject("id", UUID.class);
                    } else {
                        throw new SQLException("Не найден id в ResultSet");
                    }
                }
                userEntity.setId(id);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return userEntity;
    }

    @Override
    public Optional<UserEntity> findUser(UUID id) {
        try (Connection connection = DataBases.connection(ConfigDb.INSTANCE.jdbcUrl())) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(
                    "SELECT * FROM user WHERE id=?"
            )) {
                preparedStatement.setObject(1, id);
                preparedStatement.execute();

                try (ResultSet rs = preparedStatement.getResultSet()) {
                    if (rs.next()) {
                        UserEntity userEntity = new UserEntity();
                        userEntity.setId(rs.getObject("id", UUID.class));
                        userEntity.setName(rs.getString("name"));
                        userEntity.setEmail(rs.getString("email"));

                        return Optional.of(userEntity);
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
