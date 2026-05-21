-- 1. Добавляем поля для интеграции с Telegram в таблицу пользователей
ALTER TABLE users ADD COLUMN telegram_chat_id BIGINT;
ALTER TABLE users ADD COLUMN telegram_notifications_enabled BOOLEAN DEFAULT TRUE;

-- 2. Создаем таблицу для подписок на ключевые слова (многие-ко-многим)
CREATE TABLE user_subscribed_keywords (
    user_id BIGINT NOT NULL,
    keyword_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, keyword_id),
    CONSTRAINT fk_user_keyword_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_keyword_keyword FOREIGN KEY (keyword_id) REFERENCES search_keywords(id) ON DELETE CASCADE
);

-- 3. Создаем таблицу для подписок на грейды (коллекция строк/enum)
CREATE TABLE user_subscribed_grades (
    user_id BIGINT NOT NULL,
    grade VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, grade),
    CONSTRAINT fk_user_grade_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);