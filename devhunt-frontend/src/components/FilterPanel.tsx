import { useState, useEffect } from 'react';
import { api } from '../api'; // Убедись, что путь до api правильный

export interface FilterValues {
    keyword: string;
    direction: string;
    minSalaryRub: string;
    grade: string;
}

interface Props {
    onFilterChange: (filters: FilterValues) => void;
}

export default function FilterPanel({ onFilterChange }: Props) {
    const [filters, setFilters] = useState<FilterValues>({
        keyword: '',
        direction: '',
        minSalaryRub: '',
        grade: ''
    });

    const [availableDirections, setAvailableDirections] = useState<string[]>([]);

    // Загружаем список направлений с бэкенда при маунте компонента
    useEffect(() => {
        const fetchKeywords = async () => {
            try {
                // Если эндпоинт открытый, можно использовать обычный fetch. 
                // Если защищен токеном (даже для юзеров), юзай api.fetchWithAuth
                const res = await fetch('http://localhost:8080/api/vacancies/keywords');
                if (res.ok) {
                    const data = await res.json();
                    setAvailableDirections(data);
                }
            } catch (e) {
                console.error("Не удалось загрузить список направлений", e);
            }
        };
        fetchKeywords();
    }, []);

    // Дебаунс для текстового поиска
    useEffect(() => {
        const timer = setTimeout(() => {
            onFilterChange(filters);
        }, 500);
        return () => clearTimeout(timer);
    }, [filters, onFilterChange]);

    const handleChange = (key: keyof FilterValues, value: string) => {
        setFilters(prev => ({ ...prev, [key]: value }));
    };

    return (
        <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 mb-8 space-y-4">

            <div className="flex flex-col sm:flex-row gap-4">
                <input
                    type="text"
                    placeholder="Поиск по тексту (например, Docker)"
                    value={filters.keyword}
                    onChange={(e) => handleChange('keyword', e.target.value)}
                    className="flex-grow px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-600 focus:border-transparent outline-none transition-all text-gray-800"
                />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 border-t border-gray-100 pt-4">
                <select
                    value={filters.direction}
                    onChange={(e) => handleChange('direction', e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-600 outline-none bg-white cursor-pointer text-gray-700"
                >
                    <option value="">Все направления</option>
                    {/* Динамически рендерим список из БД */}
                    {availableDirections.map(dir => (
                        <option key={dir} value={dir}>{dir}</option>
                    ))}
                </select>

                <select
                    value={filters.grade}
                    onChange={(e) => handleChange('grade', e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-600 outline-none bg-white cursor-pointer text-gray-700"
                >
                    <option value="">Любой грейд</option>
                    <option value="INTERN">Стажировка</option>
                    <option value="JUNIOR">Junior</option>
                    <option value="MIDDLE">Middle</option>
                    <option value="SENIOR">Senior</option>
                </select>

                <input
                    type="number"
                    placeholder="Зарплата от (₽)"
                    value={filters.minSalaryRub}
                    onChange={(e) => handleChange('minSalaryRub', e.target.value)}
                    className="w-full px-4 py-3 rounded-xl border border-gray-200 focus:ring-2 focus:ring-blue-600 outline-none text-gray-700"
                />
            </div>
        </div>
    );
}