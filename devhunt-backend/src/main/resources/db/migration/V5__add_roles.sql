ALTER TABLE users ADD COLUMN role VARCHAR(20) DEFAULT 'ROLE_USER';

-- Делаем первого юзера (тебя) админом по умолчанию, чтобы было с чего начать
UPDATE users SET role = 'ROLE_ADMIN' WHERE id = 1;