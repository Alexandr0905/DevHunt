// src/components/StatsDashboard.tsx

import { useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import type { Vacancy } from '../types';

interface Props {
    vacancies: Vacancy[];
}

export default function StatsDashboard({ vacancies }: Props) {
    // Вычисляем среднюю ЗП по грейдам с помощью useMemo (кэшируем результат)
    const chartData = useMemo(() => {
        const grades: Record<string, { total: number; count: number }> = {
            JUNIOR: { total: 0, count: 0 },
            MIDDLE: { total: 0, count: 0 },
            SENIOR: { total: 0, count: 0 },
            LEAD: { total: 0, count: 0 },
        };

        vacancies.forEach(v => {
            if (v.grade && v.salaryUsd && grades[v.grade]) {
                grades[v.grade].total += v.salaryUsd;
                grades[v.grade].count += 1;
            }
        });

        return [
            { name: 'Junior', avg: grades.JUNIOR.count ? Math.round(grades.JUNIOR.total / grades.JUNIOR.count) : 0 },
            { name: 'Middle', avg: grades.MIDDLE.count ? Math.round(grades.MIDDLE.total / grades.MIDDLE.count) : 0 },
            { name: 'Senior', avg: grades.SENIOR.count ? Math.round(grades.SENIOR.total / grades.SENIOR.count) : 0 },
            { name: 'Lead', avg: grades.LEAD.count ? Math.round(grades.LEAD.total / grades.LEAD.count) : 0 },
        ];
    }, [vacancies]);

    const totalWithSalary = vacancies.filter(v => v.salaryUsd).length;

    return (
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Аналитика рынка</h2>
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
                            <YAxis axisLine={false} tickLine={false} tick={{ fill: '#6B7280' }} unit="$" />
                            <Tooltip
                                cursor={{ fill: '#F3F4F6' }}
                                contentStyle={{ borderRadius: '12px', border: 'none', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                            />
                            <Bar dataKey="avg" fill="#2563EB" radius={[6, 6, 0, 0]} name="Средняя ЗП (USD)" />
                        </BarChart>
                    </ResponsiveContainer>
                </div>
            )}
        </div>
    );
}