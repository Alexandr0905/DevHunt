// src/components/VacancyModal.tsx

import type { Vacancy } from '../types';

interface Props {
    vacancy: Vacancy | null;
    onClose: () => void;
}

export default function VacancyModal({ vacancy, onClose }: Props) {
    if (!vacancy) return null;

    return (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-md flex items-center justify-center z-50 p-4 animate-in fade-in duration-200">
            <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col relative">
                <button onClick={onClose} className="absolute top-6 right-6 p-2 bg-gray-100 hover:bg-gray-200 rounded-full transition-colors z-10">
                    <svg className="w-6 h-6 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
                </button>

                <div className="p-8 overflow-y-auto">
                    <div className="mb-6">
                        <span className="text-blue-600 font-bold text-sm uppercase tracking-wider">{vacancy.source}</span>
                        <h2 className="text-3xl font-extrabold text-gray-900 mt-2">{vacancy.title}</h2>
                        <p className="text-xl text-gray-600 font-medium">{vacancy.company}</p>
                    </div>

                    <div className="flex flex-wrap gap-3 mb-8">
                        {vacancy.salaryUsd && <span className="px-4 py-2 bg-green-50 text-green-700 rounded-xl font-bold border border-green-100">~ ${vacancy.salaryUsd}</span>}
                        {vacancy.city && <span className="px-4 py-2 bg-gray-50 text-gray-700 rounded-xl font-medium border border-gray-100">📍 {vacancy.city}</span>}
                        {vacancy.remote && <span className="px-4 py-2 bg-blue-50 text-blue-700 rounded-xl font-medium border border-blue-100">🏠 Удаленка</span>}
                        <span className="px-4 py-2 bg-purple-50 text-purple-700 rounded-xl font-medium border border-purple-100 uppercase">{vacancy.grade}</span>
                    </div>

                    <div className="prose prose-blue max-w-none">
                        <h4 className="text-lg font-bold text-gray-900 mb-3">Описание вакансии</h4>
                        <div className="text-gray-700 leading-relaxed whitespace-pre-wrap">
                            {vacancy.description || "Описание не предоставлено."}
                        </div>
                    </div>
                </div>

                <div className="p-8 bg-gray-50 border-t border-gray-100 flex gap-4">
                    <a
                        href={vacancy.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="flex-1 bg-blue-600 hover:bg-blue-700 text-white text-center font-bold py-4 rounded-2xl transition-all shadow-lg shadow-blue-200"
                    >
                        Открыть оригинал и откликнуться
                    </a>
                </div>
            </div>
        </div>
    );
}