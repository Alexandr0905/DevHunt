// import { useState } from 'react';

import { useState } from "react";

export interface FilterValues {
    keyword: string;
    direction: string; // Имя должно совпадать с бэкендом (заменили role на direction)
    minSalaryRub: string;
    grade: string;
}

interface Props {
    onFilterChange: (filters: FilterValues) => void;
    hideGrade?: boolean;
}

export default function FilterPanel({ onFilterChange, hideGrade }: Props) {
    const [keyword, setKeyword] = useState('');
    const [direction, setDirection] = useState(''); // Заменили role на direction
    const [minSalaryRub, setminSalaryRub] = useState('');
    const [grade, setGrade] = useState('');

    const handleApply = (e?: React.FormEvent) => {
        e?.preventDefault();
        onFilterChange({ keyword, direction, minSalaryRub, grade });
    };

    const handleReset = () => {
        setKeyword('');
        setDirection('');
        setminSalaryRub('');
        setGrade('');
        onFilterChange({ keyword: '', direction: '', minSalaryRub: '', grade: '' });
    };

    return (
        <form onSubmit={handleApply} className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-8">
            <div className="flex flex-col gap-4">
                {/* Верхняя строка: Свободный поиск */}
                <div className="relative">
                    <input
                        type="text"
                        placeholder="Поиск по ключевым словам (например: Docker, Kubernetes...)"
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        className="w-full pl-12 pr-4 py-3 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-500 outline-none"
                    />
                    <svg className="w-6 h-6 text-gray-400 absolute left-4 top-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                </div>

                {/* Вторая строка: Основные фильтры */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    <select
                        value={direction}
                        onChange={(e) => setDirection(e.target.value)}
                        className="border border-gray-200 rounded-xl px-4 py-2.5 bg-gray-50 outline-none"
                    >
                        <option value="">Все направления</option>
                        <option value="Java">Java Разработчик</option>
                        <option value="Python">Python Разработчик</option>
                        <option value="Frontend">Frontend (React/Vue/Angular)</option>
                        <option value="Backend">Backend (Общий)</option>
                        <option value="QA">QA / Тестировщик</option>
                        <option value="DevOps">DevOps Инженер</option>
                        <option value="Data">Data Scientist / Analyst</option>
                        <option value="Design">UI/UX Дизайнер</option>
                    </select>

                    <input
                        type="number"
                        placeholder="ЗП от (руб)"
                        value={minSalaryRub}
                        onChange={(e) => setminSalaryRub(e.target.value)}
                        className="border border-gray-200 rounded-xl px-4 py-2.5 bg-gray-50 outline-none"
                    />

                    {!hideGrade && (
                        <select
                            value={grade}
                            onChange={(e) => setGrade(e.target.value)}
                            className="border border-gray-200 rounded-xl px-4 py-2.5 bg-gray-50 outline-none"
                        >
                            <option value="">Любой грейд</option>
                            <option value="JUNIOR">Junior</option>
                            <option value="MIDDLE">Middle</option>
                            <option value="SENIOR">Senior</option>
                            <option value="LEAD">Lead</option>
                        </select>
                    )}
                </div>

                <div className="flex gap-3 mt-2">
                    <button type="submit" className="flex-1 bg-blue-600 hover:bg-blue-700 text-white font-bold py-3 rounded-xl transition-all shadow-md">
                        Применить фильтры
                    </button>
                    <button type="button" onClick={handleReset} className="px-6 py-3 bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold rounded-xl transition-all">
                        Сбросить
                    </button>
                </div>
            </div>
        </form>
    );
}