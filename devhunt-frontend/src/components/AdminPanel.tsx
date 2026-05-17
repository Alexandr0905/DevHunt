import { useEffect, useState } from 'react';
import { api } from '../api';

interface User {
    id: number;
    email: string;
    role: string;
}

interface Keyword {
    id: number;
    keyword: string;
}

export default function AdminPanel() {
    const [users, setUsers] = useState<User[]>([]);
    const [keywords, setKeywords] = useState<Keyword[]>([]);
    const [newKeyword, setNewKeyword] = useState('');
    const [loading, setLoading] = useState(true);
    const [scrapeStatus, setScrapeStatus] = useState<string | null>(null);

    // Получаем почту текущего админа из localStorage для защиты
    const currentUserEmail = localStorage.getItem('user_email');

    useEffect(() => {
        fetchData();
    }, []);

    const fetchData = async () => {
        try {
            const [usersData, keywordsData] = await Promise.all([
                api.fetchWithAuth('/admin/users'),
                api.fetchWithAuth('/admin/keywords')
            ]);
            setUsers(usersData);
            setKeywords(keywordsData);
        } catch (e) {
            console.error("Ошибка загрузки данных админки", e);
        } finally {
            setLoading(false);
        }
    };

    // --- ДЕЙСТВИЯ С ЮЗЕРАМИ ---
    const handleRoleChange = async (userId: number, currentRole: string) => {
        const newRole = currentRole === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
        try {
            await api.fetchWithAuth(`/admin/users/${userId}/role`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ role: newRole })
            });
            fetchData();
        } catch (e: any) {
            alert(e.message || "Ошибка при смене роли");
        }
    };

    const handleDeleteUser = async (userId: number, email: string) => {
        if (!window.confirm(`Точно удалить пользователя ${email}? Это сотрет все его данные.`)) return;
        try {
            await api.fetchWithAuth(`/admin/users/${userId}`, { method: 'DELETE' });
            fetchData();
        } catch (e: any) {
            alert(e.message || "Ошибка при удалении");
        }
    };

    // --- ДЕЙСТВИЯ С ПАРСЕРАМИ И СЛОВАРЕМ ---
    const handleTriggerScrape = async () => {
        try {
            setScrapeStatus("Запуск...");
            await api.fetchWithAuth('/admin/scrape', { method: 'POST' });
            setScrapeStatus("Парсеры запущены! Проверь логи бэкенда.");
            setTimeout(() => setScrapeStatus(null), 5000);
        } catch (e) {
            setScrapeStatus("Ошибка запуска парсеров");
        }
    };

    const handleAddKeyword = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!newKeyword.trim()) return;
        try {
            await api.fetchWithAuth('/admin/keywords', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ keyword: newKeyword })
            });
            setNewKeyword('');
            fetchData();
        } catch (e) {
            alert("Не удалось добавить слово (возможно, оно уже существует)");
        }
    };

    const handleDeleteKeyword = async (id: number) => {
        try {
            await api.fetchWithAuth(`/admin/keywords/${id}`, { method: 'DELETE' });
            fetchData();
        } catch (e) {
            alert("Ошибка при удалении слова");
        }
    };

    if (loading) return <div className="text-center py-20 text-gray-400 font-bold">Загрузка админки...</div>;

    return (
        <div className="space-y-8 animate-in fade-in">

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                {/* Блок управления парсерами */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex flex-col justify-between">
                    <div>
                        <h2 className="text-xl font-black text-gray-900">Сбор данных</h2>
                        <p className="text-sm text-gray-500 mt-1 mb-6">Принудительно запустить сбор свежих вакансий вне расписания</p>
                    </div>
                    <div className="flex items-center gap-4">
                        <button
                            onClick={handleTriggerScrape}
                            className="bg-gray-900 hover:bg-black text-white px-6 py-3 rounded-xl font-bold shadow-md transition-all w-full sm:w-auto"
                        >
                            ⚡ Запустить парсеры
                        </button>
                        {scrapeStatus && <span className="text-sm font-bold text-green-600 bg-green-50 px-3 py-2 rounded-lg">{scrapeStatus}</span>}
                    </div>
                </div>

                {/* Блок словаря */}
                <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                    <h2 className="text-xl font-black text-gray-900">Словарь поиска</h2>
                    <p className="text-sm text-gray-500 mt-1 mb-4">По этим словам скраперы ищут вакансии</p>

                    <form onSubmit={handleAddKeyword} className="flex gap-2 mb-4">
                        <input
                            type="text"
                            value={newKeyword}
                            onChange={(e) => setNewKeyword(e.target.value)}
                            placeholder="Новое ключевое слово..."
                            className="flex-grow px-4 py-2 rounded-xl border border-gray-200 outline-none focus:ring-2 focus:ring-blue-600"
                        />
                        <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-xl font-bold transition-colors">
                            Добавить
                        </button>
                    </form>

                    <div className="flex flex-wrap gap-2 max-h-40 overflow-y-auto p-1">
                        {keywords.map(kw => (
                            <div key={kw.id} className="flex items-center gap-2 bg-gray-100 text-gray-700 px-3 py-1.5 rounded-lg text-sm font-bold">
                                {kw.keyword}
                                <button onClick={() => handleDeleteKeyword(kw.id)} className="text-gray-400 hover:text-red-500 ml-1">✕</button>
                            </div>
                        ))}
                        {keywords.length === 0 && <span className="text-gray-400 text-sm">Словарь пуст</span>}
                    </div>
                </div>
            </div>

            {/* Таблица пользователей */}
            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                <h2 className="text-xl font-black text-gray-900 mb-6">Управление пользователями ({users.length})</h2>

                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="border-b border-gray-200 text-gray-400 text-sm uppercase tracking-wider">
                                <th className="pb-3 font-bold">ID</th>
                                <th className="pb-3 font-bold">Email</th>
                                <th className="pb-3 font-bold">Роль</th>
                                <th className="pb-3 font-bold text-right">Действия</th>
                            </tr>
                        </thead>
                        <tbody className="text-sm">
                            {users.map(u => {
                                const isMe = u.email === currentUserEmail;
                                return (
                                    <tr key={u.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                                        <td className="py-4 font-bold text-gray-500">#{u.id}</td>
                                        <td className="py-4 font-bold text-gray-900 flex items-center gap-2">
                                            {u.email}
                                            {isMe && <span className="bg-blue-100 text-blue-700 text-[10px] px-2 py-0.5 rounded-full uppercase font-black">Это вы</span>}
                                        </td>
                                        <td className="py-4">
                                            <span className={`px-3 py-1 rounded-md text-xs font-black ${u.role === 'ROLE_ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'}`}>
                                                {u.role === 'ROLE_ADMIN' ? 'АДМИН' : 'ЮЗЕР'}
                                            </span>
                                        </td>
                                        <td className="py-4 text-right space-x-3">
                                            {!isMe ? (
                                                <>
                                                    <button
                                                        onClick={() => handleRoleChange(u.id, u.role)}
                                                        className="text-blue-600 hover:text-blue-800 font-bold text-xs uppercase"
                                                    >
                                                        {u.role === 'ROLE_ADMIN' ? 'Сделать юзером' : 'Сделать админом'}
                                                    </button>
                                                    <button
                                                        onClick={() => handleDeleteUser(u.id, u.email)}
                                                        className="text-red-500 hover:text-red-700 font-bold text-xs uppercase"
                                                    >
                                                        Удалить
                                                    </button>
                                                </>
                                            ) : (
                                                <span className="text-gray-300 text-xs font-bold uppercase cursor-not-allowed">Защищено</span>
                                            )}
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}