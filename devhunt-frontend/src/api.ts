// src/api.ts

const BASE_URL = 'http://localhost:8080/api';

export const api = {
    // Базовый метод для запросов с токеном
    async fetchWithAuth(endpoint: string, options: RequestInit = {}) {
        const token = localStorage.getItem('jwt_token');

        const headers = new Headers(options.headers);
        if (token) {
            headers.set('Authorization', `Bearer ${token}`);
        }
        if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
            headers.set('Content-Type', 'application/json');
        }

        const response = await fetch(`${BASE_URL}${endpoint}`, {
            ...options,
            headers,
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Ошибка HTTP: ${response.status}`);
        }

        // Если нет контента (например, 200 OK при удалении), возвращаем null
        if (response.status === 204 || response.headers.get('content-length') === '0') {
            return null;
        }

        // Пытаемся распарсить JSON, если не выходит - возвращаем текст
        try {
            return await response.json();
        } catch {
            return null;
        }
    }
};