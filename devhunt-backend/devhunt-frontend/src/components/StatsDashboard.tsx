// src/components/StatsDashboard.tsx

import { useEffect, useState, useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import type { Vacancy } from '../types';

interface GradeInfo {
    count: number;
    avgSalaryRub: number;
}

interface DirectionStat {
    name: string;
    count: number;
    percent: number;
    grades: Record<string, GradeInfo>;
}

interface Props {
    vacancies: Vacancy[];
}

export default function StatsDashboard({ vacancies }: Props) {
    const [skills, setSkills] = useState<{ name: string, value: number }[]>([]);
    const [directions, setDirections] = useState<DirectionStat[]>([]);
    const [selectedDir, setSelectedDir] = useState<DirectionStat | null>(null);
    const [loading, setLoading] = useState(true);
    const [serverError, setServerError] = useState(false); // Флаг ошибки бэкенда

    useEffect(() => {
        const loadStats = async () => {
            try {
                const [sRes, dRes] = await Promise.all([
                    fetch('http://localhost:8080/api/vacancies/stats/skills').then(r => {
                        if (!r.ok) throw new Error('Ошибка сервера (навыки)');
                        return r.json();
                    }),
                    fetch('http://localhost:8080/api/vacancies/stats/directions').then(r => {
                        if (!r.ok) throw new Error('Ошибка сервера (направления)');
                        return r.json();
                    })
                ]);

                // Жесткая проверка: если пришел не массив, ставим пустой массив
                const safeSkills = Array.isArray(sRes) ? sRes : [];
                const safeDirections = Array.isArray(dRes) ? dRes : [];

                setSkills(safeSkills);
                setDirections(safeDirections);
                if (safeDirections.length > 0) setSelectedDir(safeDirections[0]);

            } catch (e) {
                console.error("Ошибка загрузки статистики:", e);
                setServerError(true); // Включаем показ ошибки
            } finally {
                setLoading(false);
            }
        };
        loadStats();
    }, []);

    const chartData = useMemo(() => {
        const grades: Record<string, { totalUsd: number; count: number }> = {
            JUNIOR: { totalUsd: 0, count: 0 },
            MIDDLE: { totalUsd: 0, count: 0 },
            SENIOR: { totalUsd: 0, count: 0 },
            LEAD: { totalUsd: 0, count: 0 },
        };

        (vacancies || []).forEach(v => {
            if (v.grade && v.salaryUsd && grades[v.grade]) {
                grades[v.grade].totalUsd += v.salaryUsd;
                grades[v.grade].count += 1;
            }
        });

        const usdToRub = 90;

        return [
            { name: 'Junior', avg: grades.JUNIOR.count ? Math.round((grades.JUNIOR.totalUsd / grades.JUNIOR.count) * usdToRub) : 0 },
            { name: 'Middle', avg: grades.MIDDLE.count ? Math.round((grades.MIDDLE.totalUsd / grades.MIDDLE.count) * usdToRub) : 0 },
            { name: 'Senior', avg: grades.SENIOR.count ? Math.round((grades.SENIOR.totalUsd / grades.SENIOR.count) * usdToRub) : 0 },
            { name: 'Lead', avg: grades.LEAD.count ? Math.round((grades.LEAD.totalUsd / grades.LEAD.count) * usdToRub) : 0 },
        ];
    }, [vacancies]);

    const totalWithSalary = (vacancies || []).filter(v => v.salaryUsd).length;

    if (loading) {
        return <div className="text-center py-20 text-gray-400 font-bold animate-pulse">Анализируем рынок...</div>;
    }

    if (serverError) {
        return (
            <div className="bg-red-50 border border-red-200 text-red-600 p-8 rounded-3xl text-center space-y-3">
                <div className="text-4xl">🚨</div>
                <h2 className="text-xl font-black">Бэкенд не отдал статистику</h2>
                <p className="text-sm">Скорее всего, эндпоинты аналитики еще не запущены или в коде Java произошла ошибка.</p>
                <p className="text-sm font-bold">Загляни в консоль IDEA (Spring Boot), там 100% есть StackTrace ошибки!</p>
            </div>
        );
    }

    return (
        <div className="space-y-8 animate-in fade-in duration-700">

            {/* --- БЛОК 1: ОБЩИЙ ГРАФИК --- */}
            <div className="bg-white p-8 rounded-3xl shadow-sm border border-gray-100">
                <h2 className="text-2xl font-black text-gray-900 mb-2">Аналитика рынка</h2>
                <p className="text-gray-500 mb-8">
                    Средняя заработная плата по грейдам (на основе {totalWithSalary} вакансий с указанной ЗП)
                </p>

                {totalWithSalary === 0 ? (
                    <div className="text-center text-gray-400 py-10 bg-gray-50 rounded-xl">
                        Недостаточно данных для построения графика
                    </div>
                ) : (
                    <div className="h-80 w-full">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={chartData} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#E5E7EB" />
                                <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#6B7280', fontWeight: 600 }} />
                                <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280' }} tickFormatter={(value) => `${value / 1000}k ₽`} />
                                <Tooltip
                                    cursor={{ fill: '#F3F4F6' }}
                                    formatter={(value: number) => [`${value.toLocaleString()} ₽`, 'Средняя ЗП']}
                                    contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                />
                                <Bar dataKey="avg" fill="#2563EB" radius={[6, 6, 0, 0]} name="Средняя ЗП" />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                )}
            </div>

            {/* --- БЛОК 2: SKILL SCANNER --- */}
            <div className="bg-white p-8 rounded-3xl border border-gray-100 shadow-sm">
                <h3 className="text-xl font-black mb-6 flex items-center gap-2">
                    <span className="text-2xl">🔥</span> Популярные технологии
                </h3>
                <div className="flex flex-wrap gap-3">
                    {skills.map(s => (
                        <div key={s.name} className="px-4 py-2 bg-blue-50 text-blue-700 rounded-xl text-sm font-bold border border-blue-100 hover:bg-blue-100 transition-colors cursor-default">
                            {s.name} <span className="ml-2 text-blue-400">{s.value}</span>
                        </div>
                    ))}
                    {skills.length === 0 && <span className="text-gray-400 text-sm">Нет данных по навыкам</span>}
                </div>
            </div>

            {/* --- БЛОК 3: НАПРАВЛЕНИЯ --- */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-1 space-y-3">
                    <h3 className="text-lg font-black mb-4 px-2">Спрос по направлениям</h3>
                    {directions.map(d => (
                        <button
                            key={d.name}
                            onClick={() => setSelectedDir(d)}
                            className={`w-full text-left p-4 rounded-2xl transition-all border ${selectedDir?.name === d.name
                                    ? 'bg-blue-600 border-blue-600 text-white shadow-lg shadow-blue-200 scale-[1.02]'
                                    : 'bg-white border-gray-100 text-gray-600 hover:border-blue-300'
                                }`}
                        >
                            <div className="flex justify-between items-center">
                                <span className="font-bold">{d.name}</span>
                                <span className={`text-xs font-black ${selectedDir?.name === d.name ? 'text-blue-100' : 'text-blue-600'}`}>
                                    {d.percent?.toFixed(1) || 0}%
                                </span>
                            </div>
                            <div className={`text-xs mt-1 ${selectedDir?.name === d.name ? 'text-blue-200' : 'text-gray-400'}`}>
                                {d.count} вакансий
                            </div>
                        </button>
                    ))}
                    {directions.length === 0 && <div className="text-gray-400 text-sm px-2">Нет данных по направлениям</div>}
                </div>

                <div className="lg:col-span-2">
                    <div className="bg-gray-900 rounded-3xl p-8 text-white h-full shadow-2xl relative overflow-hidden">
                        <div className="absolute top-0 right-0 w-32 h-32 bg-blue-500/10 rounded-full -mr-16 -mt-16 blur-3xl"></div>

                        {selectedDir ? (
                            <>
                                <div className="flex justify-between items-start mb-8 relative z-10">
                                    <div>
                                        <h3 className="text-2xl font-black text-blue-400">{selectedDir.name}</h3>
                                        <p className="text-gray-400 text-sm mt-1">Анализ зарплат и грейдов внутри направления</p>
                                    </div>
                                    <div className="bg-gray-800 px-4 py-2 rounded-xl border border-gray-700 text-center">
                                        <span className="text-[10px] text-gray-500 uppercase font-black tracking-wider block mb-1">Всего найдено</span>
                                        <span className="text-xl font-black text-white">{selectedDir.count}</span>
                                    </div>
                                </div>

                                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 relative z-10">
                                    {selectedDir.grades && Object.entries(selectedDir.grades).map(([grade, info]) => (
                                        <div key={grade} className="bg-gray-800/60 p-5 rounded-2xl border border-gray-700 hover:border-blue-500/50 transition-colors">
                                            <div className="flex justify-between items-center mb-4">
                                                <span className="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-[10px] font-black uppercase tracking-widest">
                                                    {grade}
                                                </span>
                                                <span className="text-xs font-bold text-gray-500 bg-gray-900 px-2 py-1 rounded-md">
                                                    {info.count} шт.
                                                </span>
                                            </div>
                                            <div className="text-2xl font-black text-white tracking-tight">
                                                {info.avgSalaryRub > 0 ? info.avgSalaryRub.toLocaleString() + ' ₽' : '—'}
                                            </div>
                                            <p className="text-[10px] text-gray-500 mt-2 uppercase font-bold tracking-widest">Средняя зарплата</p>
                                        </div>
                                    ))}
                                    {(!selectedDir.grades || Object.keys(selectedDir.grades).length === 0) && (
                                        <div className="col-span-full text-center py-10 text-gray-500 italic">
                                            Детальная статистика по грейдам пока недоступна
                                        </div>
                                    )}
                                </div>
                            </>
                        ) : (
                            <div className="h-full flex items-center justify-center text-gray-600 italic">
                                Данные для детализации отсутствуют
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}