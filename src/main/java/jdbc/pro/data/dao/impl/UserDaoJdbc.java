package jdbc.pro.data.dao.impl;

import jdbc.pro.data.dao.UserDao;
import jdbc.pro.data.entity.user.UserEntity;

import java.sql.*;
import java.util.Optional;
import java.util.UUID;

public class UserDaoJdbc implements UserDao {

    private final Connection connection;

    public UserDaoJdbc(Connection connection) {
        this.connection = connection;
    }

    @Override
    public UserEntity create(UserEntity userEntity) {
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
            return userEntity;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<UserEntity> findUser(UUID id) {
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
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
