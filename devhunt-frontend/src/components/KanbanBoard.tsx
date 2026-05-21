// src/components/KanbanBoard.tsx

import { useEffect, useState } from 'react';
import type { JobApplication, ApplicationStatus, Vacancy } from '../types';
import { api } from '../api';

interface Props {
    onSelectVacancy: (vacancy: Vacancy) => void;
}

const COLUMNS: { id: ApplicationStatus; title: string; color: string }[] = [
    { id: 'VIEWED', title: 'Просмотрено 👀', color: 'bg-gray-100 border-gray-200 text-gray-800' },
    { id: 'APPLIED', title: 'Скрининг / Отклик', color: 'bg-blue-100 border-blue-200 text-blue-800' },
    { id: 'INTERVIEW', title: 'Интервью 🗣', color: 'bg-yellow-100 border-yellow-200 text-yellow-800' },
    { id: 'OFFER', title: 'Оффер 💸', color: 'bg-green-100 border-green-200 text-green-800' },
    { id: 'REJECTED', title: 'Отказ ❌', color: 'bg-red-100 border-red-200 text-red-800' }
];

export default function KanbanBoard({ onSelectVacancy }: Props) {
    const [applications, setApplications] = useState<JobApplication[]>([]);
    const [loading, setLoading] = useState(true);

    const [collapsedCols, setCollapsedCols] = useState<Set<ApplicationStatus>>(new Set());

    useEffect(() => {
        fetchBoard();
    }, []);

    const fetchBoard = async () => {
        try {
            const data = await api.fetchWithAuth('/kanban');
            setApplications(data);
        } catch (e) {
            console.error("Ошибка загрузки канбана", e);
        } finally {
            setLoading(false);
        }
    };

    const changeStatus = async (appId: number, newStatus: ApplicationStatus) => {
        try {
            setApplications(prev => prev.map(app =>
                app.id === appId ? { ...app, status: newStatus } : app
            ));

            await api.fetchWithAuth(`/kanban/${appId}/status`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ status: newStatus })
            });
        } catch (e) {
            console.error("Ошибка при смене статуса", e);
            fetchBoard();
        }
    };

    const deleteApp = async (appId: number) => {
        if (!window.confirm("Удалить вакансию из трекера?")) return;
        try {
            await api.fetchWithAuth(`/kanban/${appId}`, { method: 'DELETE' });
            setApplications(prev => prev.filter(a => a.id !== appId));
        } catch (e) {
            console.error("Ошибка при удалении", e);
        }
    };

    const toggleColumn = (colId: ApplicationStatus) => {
        setCollapsedCols(prev => {
            const next = new Set(prev);
            if (next.has(colId)) next.delete(colId);
            else next.add(colId);
            return next;
        });
    };

    if (loading) return <div className="text-center py-20 animate-pulse text-gray-500 font-bold">Загрузка доски...</div>;

    return (
        <div className="p-2 sm:p-6 flex flex-col h-full">
            <h2 className="text-3xl font-black mb-8 text-gray-800 shrink-0">Мои отклики</h2>

            {/* ИСПРАВЛЕНО: Сетка на 3 колонки. Первые 3 займут верхний ряд, оставшиеся 2 перенесутся вниз */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 items-start pb-8">
                {COLUMNS.map(col => {
                    const isCollapsed = collapsedCols.has(col.id);
                    const colApps = applications.filter(a => a.status === col.id);

                    return (
                        <div key={col.id} className="w-full bg-gray-50 rounded-2xl p-4 border border-gray-200 flex flex-col max-h-[75vh] transition-all shadow-sm">

                            {/* Шапка колонки */}
                            <div
                                onClick={() => toggleColumn(col.id)}
                                className="flex justify-between items-center mb-3 px-2 cursor-pointer hover:bg-gray-200 p-2 rounded-xl transition-colors group select-none"
                            >
                                <div className="flex items-center gap-2">
                                    <svg className={`w-5 h-5 text-gray-400 group-hover:text-gray-600 transition-transform ${isCollapsed ? '-rotate-90' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                                    </svg>
                                    <h3 className="font-black text-gray-700 text-sm uppercase tracking-wide">{col.title}</h3>
                                </div>
                                <span className="bg-white px-2 py-1 rounded-md text-xs font-black shadow-sm text-gray-500 border border-gray-100">
                                    {colApps.length}
                                </span>
                            </div>

                            {/* Список карточек */}
                            {!isCollapsed && (
                                <div className="space-y-3 overflow-y-auto pr-1 pb-2 scroll-smooth">
                                    {colApps.map(app => (
                                        <div
                                            key={app.id}
                                            onClick={() => onSelectVacancy(app.vacancy)}
                                            className={`p-4 rounded-xl border bg-white shadow-sm transition-all hover:shadow-md relative group cursor-pointer ${col.color}`}
                                        >
                                            <button
                                                onClick={(e) => { e.stopPropagation(); deleteApp(app.id); }}
                                                className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 p-1 text-gray-400 hover:text-red-500 transition-all bg-white rounded-lg shadow-sm border border-gray-100"
                                                title="Удалить из трекера"
                                            >
                                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                                </svg>
                                            </button>

                                            <div className="font-bold text-sm mb-1 line-clamp-2 pr-6 leading-snug">{app.vacancy.title}</div>
                                            <div className="text-xs opacity-70 font-bold mb-3 line-clamp-1">{app.vacancy.company}</div>

                                            {app.vacancy.salaryUsd && (
                                                <div className="text-sm font-black mb-3">
                                                    {(app.vacancy.salaryUsd * 90).toLocaleString()} ₽
                                                </div>
                                            )}

                                            <a
                                                href={app.vacancy.url}
                                                target="_blank"
                                                rel="noopener noreferrer"
                                                onClick={(e) => e.stopPropagation()}
                                                className="block w-full text-center py-1.5 mb-3 bg-white/50 border border-black/5 hover:bg-white hover:border-blue-200 rounded-lg text-xs font-bold transition-all"
                                            >
                                                На сайт
                                            </a>

                                            <div className="flex flex-wrap gap-1.5">
                                                {COLUMNS.map(targetCol => {
                                                    if (targetCol.id === col.id) return null;
                                                    return (
                                                        <button
                                                            key={targetCol.id}
                                                            onClick={(e) => { e.stopPropagation(); changeStatus(app.id, targetCol.id); }}
                                                            className="text-[9px] font-bold px-2 py-1 bg-white border border-gray-200 rounded text-gray-600 hover:bg-gray-100 hover:border-gray-300 transition-all uppercase shadow-sm"
                                                        >
                                                            {targetCol.title.split(' ')[0]}
                                                        </button>
                                                    );
                                                })}
                                            </div>
                                        </div>
                                    ))}
                                    {colApps.length === 0 && (
                                        <div className="text-center text-xs text-gray-400 py-8 border-2 border-dashed border-gray-200 rounded-xl font-bold bg-white/50">
                                            Пока пусто
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}