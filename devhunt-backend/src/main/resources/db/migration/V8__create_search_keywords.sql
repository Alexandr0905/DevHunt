CREATE TABLE search_keywords (
    id BIGSERIAL PRIMARY KEY,
    keyword VARCHAR(100) UNIQUE NOT NULL
);

INSERT INTO search_keywords (keyword) VALUES
('React'), ('Java'), ('Spring'), ('Python'), ('Frontend'), ('Backend'), ('QA');