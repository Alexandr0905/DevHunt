CREATE TABLE job_applications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    vacancy_id BIGINT NOT NULL REFERENCES vacancies(id),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE(user_id, vacancy_id) -- Чтобы нельзя было откликнуться дважды на одну вакансию
);