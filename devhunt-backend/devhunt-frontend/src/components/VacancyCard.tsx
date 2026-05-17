// src/components/VacancyCard.tsx

import type { Vacancy } from '../types';

interface Props {
    vacancy: Vacancy;
    isFavorite?: boolean;
    onToggleFavorite?: (vacancyId: number) => void;
    isAuthenticated: boolean;
}

export default function VacancyCard({ vacancy, isFavorite, onToggleFavorite, isAuthenticated }: Props) {
    return (
        <div className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition-all relative group">

            {/* Кнопка добавления в избранное */}
            {isAuthenticated && onToggleFavorite && (
                <button
                    onClick={(e) => {
                        e.stopPropagation(); // <-- ВОТ ОНО! Блокируем всплытие клика к родителю
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
                {/* Кнопке отклика тоже нужно запретить всплытие, чтобы не открывалась модалка при переходе по ссылке */}
                <a
                    href={vacancy.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                    className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-5 rounded-lg transition-colors"
                >
                    Откликнуться
                </a>
            </div>
        </div>
    );
}