-- Создание тестовой таблицы
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Вставка тестовых данных
INSERT INTO users (name, email) VALUES
('Иван Иванов', 'ivan@test.com'),
('Петр Петров', 'petr@test.com'),
('Анна Сидорова', 'anna@test.com');