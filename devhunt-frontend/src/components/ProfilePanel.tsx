import { useEffect, useState } from 'react';
import { api } from '../api';

export default function ProfilePanel() {
    const [userEmail, setUserEmail] = useState<string | null>(localStorage.getItem('user_email'));
    const [tgStatus, setTgStatus] = useState({ connected: false, enabled: false });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    // Стейты для чипсов (фильтров)
    const [allKeywords, setAllKeywords] = useState<string[]>([]);
    const [selectedKeywords, setSelectedKeywords] = useState<Set<string>>(new Set());
    const [selectedGrades, setSelectedGrades] = useState<Set<string>>(new Set());

    // Все возможные грейды в системе
    const ALL_GRADES = [
        { id: 'INTERN', label: 'Стажировка' },
        { id: 'JUNIOR', label: 'Junior' },
        { id: 'MIDDLE', label: 'Middle' },
        { id: 'SENIOR', label: 'Senior' },
        { id: 'LEAD', label: 'Lead' }
    ];

    useEffect(() => {
        fetchTelegramStatus();
    }, []);

    const fetchTelegramStatus = async () => {
        try {
            // Запрашиваем базовый статус
            const statusData = await api.fetchWithAuth('/users/profile/telegram-status');
            setTgStatus(statusData);

            // Если ТГ подключен, тянем настройки фильтров
            if (statusData.connected) {
                const settings = await api.fetchWithAuth('/users/profile/telegram-settings');
                setAllKeywords(settings.allKeywords || []);
                setSelectedKeywords(new Set(settings.selectedKeywords || []));
                setSelectedGrades(new Set(settings.selectedGrades || []));
            }
        } catch (e) {
            console.error("Не удалось получить статус Telegram", e);
        } finally {
            setLoading(false);
        }
    };

    const handleConnectTelegram = async () => {
        try {
            const res = await api.fetchWithAuth('/users/profile/telegram-token');
            window.open(res.link, '_blank');

            const interval = setInterval(async () => {
                const data = await api.fetchWithAuth('/users/profile/telegram-status');
                if (data.connected) {
                    setTgStatus(data);
                    fetchTelegramStatus(); // Подтягиваем фильтры после подключения
                    clearInterval(interval);
                }
            }, 3000);
            setTimeout(() => clearInterval(interval), 120000);
        } catch (e) {
            alert("Не удалось сгенерировать ссылку для Telegram.");
        }
    };

    const handleDisconnectTelegram = async () => {
        if (!window.confirm("Точно отключить уведомления в Telegram?")) return;
        try {
            await api.fetchWithAuth('/users/profile/telegram', { method: 'DELETE' });
            fetchTelegramStatus();
        } catch (e) {
            console.error("Ошибка при отключении", e);
        }
    };

    // --- Обработчики кликов по чипсам ---
    const toggleKeyword = (kw: string) => {
        setSelectedKeywords(prev => {
            const next = new Set(prev);
            if (next.has(kw)) next.delete(kw);
            else next.add(kw);
            return next;
        });
    };

    const toggleGrade = (gradeId: string) => {
        setSelectedGrades(prev => {
            const next = new Set(prev);
            if (next.has(gradeId)) next.delete(gradeId);
            else next.add(gradeId);
            return next;
        });
    };

    // Отправка настроек на бэк
    const saveSettings = async () => {
        setSaving(true);
        try {
            await api.fetchWithAuth('/users/profile/telegram-settings', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    enabled: tgStatus.enabled,
                    selectedKeywords: Array.from(selectedKeywords),
                    selectedGrades: Array.from(selectedGrades)
                })
            });
            alert("Настройки успешно сохранены!");
        } catch (e) {
            alert("Ошибка при сохранении настроек.");
        } finally {
            setSaving(false);
        }
    };

    const toggleGlobalNotifications = () => {
        setTgStatus(prev => ({ ...prev, enabled: !prev.enabled }));
    };


    if (loading) return <div className="p-6 text-center text-gray-500 font-bold animate-pulse">Загрузка профиля...</div>;

    return (
        <div className="bg-white rounded-2xl p-6 md:p-10 shadow-sm border border-gray-100 mb-8 max-w-2xl mx-auto">
            <h2 className="text-3xl font-black mb-8 text-gray-900">Настройки профиля</h2>

            <div className="mb-8 p-6 bg-gray-50 rounded-xl border border-gray-100 flex items-center gap-4">
                <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-2xl font-black shadow-inner">
                    {userEmail?.charAt(0).toUpperCase()}
                </div>
                <div>
                    <div className="text-sm font-bold text-gray-400 mb-1">Ваш аккаунт</div>
                    <div className="text-xl font-black text-gray-800">{userEmail}</div>
                </div>
            </div>

            <div className="border-t border-gray-100 pt-8">
                <div className="flex justify-between items-start mb-6">
                    <div>
                        <h3 className="text-xl font-black text-gray-900 mb-2 flex items-center gap-2">
                            <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path>
                            </svg>
                            Уведомления Telegram
                        </h3>
                        <p className="text-gray-500 text-sm leading-relaxed font-medium max-w-md">
                            Бот пришлет пуш-уведомление, как только на рынке появится свежая вакансия по вашим фильтрам.
                        </p>
                    </div>
                </div>

                {tgStatus.connected ? (
                    <div className="space-y-6">
                        <div className="flex flex-col sm:flex-row items-center justify-between gap-4 bg-gray-50 p-4 rounded-xl border border-gray-200">
                            <div className="flex items-center gap-3 w-full">
                                <div className="w-10 h-10 bg-green-100 text-green-600 rounded-full flex items-center justify-center">
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path></svg>
                                </div>
                                <div>
                                    <div className="font-bold text-gray-900">Бот привязан к аккаунту</div>
                                    <div className="text-xs text-gray-500 font-medium cursor-pointer text-red-500 hover:underline" onClick={handleDisconnectTelegram}>Отвязать бота</div>
                                </div>
                            </div>

                            {/* Главный рубильник (Тумблер) */}
                            <label className="relative inline-flex items-center cursor-pointer shrink-0">
                                <input type="checkbox" checked={tgStatus.enabled} onChange={toggleGlobalNotifications} className="sr-only peer" />
                                <div className="w-14 h-7 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-6 after:w-6 after:transition-all peer-checked:bg-blue-600 shadow-inner"></div>
                                <span className="ml-3 text-sm font-bold text-gray-700">{tgStatus.enabled ? 'Включены' : 'Пауза'}</span>
                            </label>
                        </div>

                        {/* Блок фильтров показывается только если бот подключен */}
                        <div className={`transition-opacity ${!tgStatus.enabled ? 'opacity-50 pointer-events-none' : ''}`}>
                            <div className="mb-6">
                                <h4 className="font-bold text-gray-800 mb-3">Интересующие технологии</h4>
                                <div className="flex flex-wrap gap-2">
                                    {allKeywords.map(kw => {
                                        const isActive = selectedKeywords.has(kw);
                                        return (
                                            <button
                                                key={kw}
                                                onClick={() => toggleKeyword(kw)}
                                                className={`px-4 py-2 rounded-xl text-sm font-bold border transition-all ${isActive
                                                        ? 'bg-blue-100 border-blue-300 text-blue-800 shadow-sm'
                                                        : 'bg-white border-gray-200 text-gray-600 hover:border-blue-200 hover:bg-blue-50'
                                                    }`}
                                            >
                                                {isActive && '✓ '} {kw}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            <div className="mb-8">
                                <h4 className="font-bold text-gray-800 mb-3">Ваш грейд</h4>
                                <div className="flex flex-wrap gap-2">
                                    {ALL_GRADES.map(grade => {
                                        const isActive = selectedGrades.has(grade.id);
                                        return (
                                            <button
                                                key={grade.id}
                                                onClick={() => toggleGrade(grade.id)}
                                                className={`px-4 py-2 rounded-xl text-sm font-bold border transition-all ${isActive
                                                        ? 'bg-purple-100 border-purple-300 text-purple-800 shadow-sm'
                                                        : 'bg-white border-gray-200 text-gray-600 hover:border-purple-200 hover:bg-purple-50'
                                                    }`}
                                            >
                                                {isActive && '✓ '} {grade.label}
                                            </button>
                                        );
                                    })}
                                </div>
                            </div>

                            <button
                                onClick={saveSettings}
                                disabled={saving}
                                className="w-full py-3 bg-gray-900 text-white font-black rounded-xl hover:bg-gray-800 transition-all shadow-md flex justify-center items-center gap-2 disabled:opacity-70"
                            >
                                {saving ? 'Сохранение...' : 'Сохранить настройки фильтров'}
                            </button>
                        </div>
                    </div>
                ) : (
                    <button
                        onClick={handleConnectTelegram}
                        className="w-full sm:w-auto px-8 py-3 bg-blue-600 text-white font-black rounded-xl hover:bg-blue-700 transition-all shadow-md hover:shadow-lg hover:-translate-y-0.5 flex items-center justify-center gap-3"
                    >
                        <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 24 24">
                            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69a.2.2 0 00-.05-.18c-.06-.05-.14-.03-.21-.02-.09.02-1.49.95-4.22 2.79-.4.27-.76.41-1.08.4-.36-.01-1.04-.2-1.55-.37-.63-.2-1.12-.31-1.08-.66.02-.18.27-.36.74-.55 2.92-1.27 4.86-2.11 5.83-2.51 2.78-1.16 3.35-1.36 3.73-1.36.08 0 .27.02.39.12.1.08.13.19.14.27-.01.06.01.24 0 .38z" />
                        </svg>
                        Подключить Telegram-бота
                    </button>
                )}
            </div>
        </div>
    );
}