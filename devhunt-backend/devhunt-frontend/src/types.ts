// src/types.ts

export interface Vacancy {
    id: number;
    title: string;
    company: string;
    salaryOrig?: string;
    salaryUsd?: number;
    description?: string;
    url: string;
    source: string;
    city?: string;
    remote: boolean;
    grade?: string;
    activeStatus: boolean;
    createdAt: string;
}

// Spring Data JPA возвращает данные в объекте Page
export interface PageResponse<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number; // текущая страница
}

export interface UserProfile {
    email: string;
    emailNotifications: boolean;
    dailyDigest: boolean;
}