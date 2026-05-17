// src/components/AuthModal.tsx

import { useState } from 'react';
import { api } from '../api';

interface Props {
    isOpen: boolean;
    onClose: () => void;
    onLoginSuccess: (token: string, email: string) => void;
}

export default function AuthModal({ isOpen, onClose, onLoginSuccess }: Props) {
    const [isLoginMode, setIsLoginMode] = useState(true);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    if (!isOpen) return null;

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const endpoint = isLoginMode ? '/auth/login' : '/auth/register';
            const response = await fetch(`http://localhost:8080/api${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password }),
            });

            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || 'Ошибка авторизации');
            }

            if (isLoginMode) {
                const data = await response.json();
                onLoginSuccess(data.token, data.email);
                onClose();
            } else {
                // Если это регистрация, сразу переключаем на форму логина
                setIsLoginMode(true);
                setError('Успешно! Теперь войдите в систему.');
            }
        } catch (err: any) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 relative">
                <button
                    onClick={onClose}
                    className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 text-2xl font-bold"
                >
                    &times;
                </button>

                <h2 className="text-2xl font-bold text-center mb-6">
                    {isLoginMode ? 'Вход в аккаунт' : 'Регистрация'}
                </h2>

                {error && (
                    <div className={`p-3 rounded-lg text-sm mb-4 text-center font-medium ${error.includes('Успешно') ? 'bg-green-50 text-green-600' : 'bg-red-50 text-red-600'}`}>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Email</label>
                        <input
                            type="email"
                            required
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500"
                        />
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-700 mb-1">Пароль</label>
                        <input
                            type="password"
                            required
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500"
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={loading}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2.5 rounded-lg transition-colors disabled:opacity-50"
                    >
                        {loading ? 'Подождите...' : (isLoginMode ? 'Войти' : 'Создать аккаунт')}
                    </button>
                </form>

                <p className="text-center mt-6 text-sm text-gray-600">
                    {isLoginMode ? 'Нет аккаунта? ' : 'Уже есть аккаунт? '}
                    <button
                        type="button"
                        onClick={() => { setIsLoginMode(!isLoginMode); setError(''); }}
                        className="text-blue-600 font-semibold hover:underline"
                    >
                        {isLoginMode ? 'Зарегистрироваться' : 'Войти'}
                    </button>
                </p>
            </div>
        </div>
    );
}