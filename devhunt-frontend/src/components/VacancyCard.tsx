// src/components/VacancyCard.tsx

import type { Vacancy } from '../types';

interface Props {
    vacancy: Vacancy;
    isFavorite?: boolean;
    onToggleFavorite?: (vacancyId: number) => void;
    isAuthenticated: boolean;
    isTracked?: boolean;
    onTrack?: (vacancyId: number) => void;
    onUntrack?: (vacancyId: number) => void; // 1. Добавили в интерфейс
}

// 2. Добавили onUntrack в список принимаемых параметров
export default function VacancyCard({
    vacancy,
    isFavorite,
    onToggleFavorite,
    isAuthenticated,
    isTracked,
    onTrack,
    onUntrack
}: Props) {
    return (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-all relative group">

            {isAuthenticated && onToggleFavorite && (
                <button
                    onClick={(e) => {
                        e.stopPropagation();
                        onToggleFavorite(vacancy.id);
                    }}
                    className="absolute top-4 right-4 p-2 rounded-full hover:bg-gray-50 transition-colors z-10"
                    title={isFavorite ? "Убрать из избранного" : "Добавить в избранное"}
                >
                    <svg
                        className={`w-6 h-6 transition-colors ${isFavorite ? 'text-red-500 fill-current' : 'text-gray-300 stroke-current fill-none hover:text-red-400'}`}
                        viewBox="0 0 24 24" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
                    >
                        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                    </svg>
                </button>
            )}

            <div className="flex flex-col sm:flex-row justify-between sm:items-start gap-4 mb-4 pr-10">
                <div>
                    <h3 className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">{vacancy.title}</h3>
                    <p className="text-gray-600 font-medium mt-1">{vacancy.company}</p>
                </div>
                {vacancy.salaryOrig ? (
                    <span className="bg-green-50 text-green-700 border border-green-200 text-sm font-semibold px-3 py-1 rounded-full whitespace-nowrap">
                        {vacancy.salaryOrig} {vacancy.salaryUsd ? <span className="text-green-500 ml-1">(~ ${vacancy.salaryUsd})</span> : ''}
                    </span>
                ) : vacancy.salaryUsd ? (
                    <span className="bg-green-50 text-green-700 border border-green-200 text-sm font-semibold px-3 py-1 rounded-full whitespace-nowrap">
                        ~ ${vacancy.salaryUsd}
                    </span>
                ) : null}
            </div>

            <div className="flex flex-wrap gap-2 mb-6 text-sm">
                {vacancy.city && <span className="bg-gray-100 text-gray-700 px-2 py-1 rounded">📍 {vacancy.city}</span>}
                {vacancy.remote && <span className="bg-blue-50 text-blue-700 border border-blue-100 px-2 py-1 rounded">🏠 Удаленка</span>}
                {vacancy.grade && vacancy.grade !== 'UNKNOWN' && (
                    <span className="bg-purple-50 text-purple-700 border border-purple-100 px-2 py-1 rounded font-medium">🎓 {vacancy.grade}</span>
                )}
            </div>

            <div className="flex justify-between items-center mt-4 border-t border-gray-100 pt-4">
                <span className="text-xs text-gray-400 font-medium tracking-wide uppercase">
                    Источник: {vacancy.source}
                </span>

                <div className="flex gap-3 items-center">
                    {/* 3. Логика с использованием onUntrack */}
                    {isTracked ? (
                        <div className="flex items-center gap-2">
                            <span className="px-3 py-2 bg-gray-100 text-gray-500 font-bold rounded-lg text-sm flex items-center gap-2 cursor-default">
                                👀 Просмотрено
                            </span>
                            <button
                                onClick={(e) => {
                                    e.stopPropagation();
                                    if (onUntrack) onUntrack(vacancy.id);
                                }}
                                className="p-2 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all"
                                title="Отменить просмотр"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>
                    ) : (
                        <button
                            onClick={(e) => {
                                e.stopPropagation();
                                if (onTrack) onTrack(vacancy.id);
                            }}
                            className="bg-blue-50 text-blue-600 hover:bg-blue-100 font-bold py-2 px-4 rounded-lg transition-colors text-sm"
                        >
                            В трекер
                        </button>
                    )}

                    <a
                        href={vacancy.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        onClick={(e) => {
                            e.stopPropagation();
                            if (onTrack && !isTracked) {
                                onTrack(vacancy.id);
                            }
                        }}
                        className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-5 rounded-lg transition-colors text-sm"
                    >
                        На сайт
                    </a>
                </div>
            </div>
        </div>
    );
}