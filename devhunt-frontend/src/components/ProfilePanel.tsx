// src/components/ProfilePanel.tsx
import { useState, useEffect } from 'react';
import { api } from '../api';
import type { UserProfile } from '../types';

export default function ProfilePanel() {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [loading, setLoading] = useState(true);

    // Загружаем реальные данные из БД при открытии профиля
    useEffect(() => {
        api.fetchWithAuth('/users/profile')
            .then(data => {
                setProfile(data);
                setLoading(false);
            })
            .catch(err => console.error(err));
    }, []);

    // Отправляем изменения в БД
    const toggleSetting = async (field: 'emailNotifications' | 'dailyDigest') => {
        if (!profile) return;

        const updatedProfile = { ...profile, [field]: !profile[field] };
        // Оптимистичный UI: обновляем сразу, чтобы не было задержки
        setProfile(updatedProfile);

        try {
            await api.fetchWithAuth('/users/profile', {
                method: 'PUT',
                body: JSON.stringify(updatedProfile)
            });
        } catch (err) {
            alert("Ошибка при сохранении настроек");
            // Откатываем назад при ошибке
            setProfile(profile);
        }
    };

    if (loading || !profile) return <div className="text-center py-10">Загрузка профиля...</div>;

    return (
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
            <div className="flex items-center gap-4 mb-8 pb-8 border-b border-gray-100">
                <div className="w-16 h-16 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-2xl font-bold">
                    {profile.email.charAt(0).toUpperCase()}
                </div>
                <div>
                    <h2 className="text-2xl font-bold text-gray-900">Мой профиль</h2>
                    <p className="text-gray-500">{profile.email}</p>
                </div>
            </div>

            <div className="space-y-6">
                <h3 className="text-lg font-bold text-gray-900">Настройки уведомлений (сохраняются автоматически)</h3>

                <label className="flex items-center justify-between cursor-pointer p-4 hover:bg-gray-50 rounded-xl transition-colors border border-transparent hover:border-gray-200">
                    <div>
                        <div className="font-semibold text-gray-900">Уведомления о новых вакансиях</div>
                        <div className="text-sm text-gray-500">Получать email при совпадении с избранными фильтрами</div>
                    </div>
                    <div className={`w-12 h-6 rounded-full p-1 transition-colors ${profile.emailNotifications ? 'bg-blue-600' : 'bg-gray-300'}`} onClick={() => toggleSetting('emailNotifications')}>
                        <div className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${profile.emailNotifications ? 'translate-x-6' : 'translate-x-0'}`}></div>
                    </div>
                </label>

                <label className="flex items-center justify-between cursor-pointer p-4 hover:bg-gray-50 rounded-xl transition-colors border border-transparent hover:border-gray-200">
                    <div>
                        <div className="font-semibold text-gray-900">Ежедневный дайджест</div>
                        <div className="text-sm text-gray-500">Сводка лучших вакансий за день</div>
                    </div>
                    <div className={`w-12 h-6 rounded-full p-1 transition-colors ${profile.dailyDigest ? 'bg-blue-600' : 'bg-gray-300'}`} onClick={() => toggleSetting('dailyDigest')}>
                        <div className={`bg-white w-4 h-4 rounded-full shadow-md transform transition-transform ${profile.dailyDigest ? 'translate-x-6' : 'translate-x-0'}`}></div>
                    </div>
                </label>
            </div>
        </div>
    );
}