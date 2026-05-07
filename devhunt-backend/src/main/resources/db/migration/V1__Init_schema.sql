CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE vacancies (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    company VARCHAR(255) NOT NULL,
    salary_orig VARCHAR(100),
    salary_usd NUMERIC(10, 2),
    description TEXT,
    url VARCHAR(500) NOT NULL UNIQUE,
    source VARCHAR(100) NOT NULL,
    city VARCHAR(100),
    remote BOOLEAN DEFAULT FALSE,
    grade VARCHAR(50),
    active_status BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_favorites (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, vacancy_id)
);