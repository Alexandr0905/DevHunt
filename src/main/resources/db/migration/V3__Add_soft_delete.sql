-- Добавляем колонку deleted_at, если её нет
ALTER TABLE vacancies ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- Убеждаемся, что у active_status стоит значение по умолчанию
ALTER TABLE vacancies ALTER COLUMN active_status SET DEFAULT TRUE;