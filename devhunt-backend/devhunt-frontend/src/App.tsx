// src/App.tsx

import { useEffect, useState } from 'react';
import { api } from './api';
import type { Vacancy, PageResponse } from './types';
import VacancyCard from './components/VacancyCard';
import FilterPanel, { type FilterValues } from './components/FilterPanel';
import AuthModal from './components/AuthModal';
import VacancyModal from './components/VacancyModal';
import StatsDashboard from './components/StatsDashboard';
import ProfilePanel from './components/ProfilePanel';

type ViewMode = 'all' | 'favorites' | 'stats' | 'profile' | 'internships';

function App() {
  const [vacancies, setVacancies] = useState<Vacancy[]>([]);
  const [favoriteIds, setFavoriteIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [selectedVacancy, setSelectedVacancy] = useState<Vacancy | null>(null);
  const [viewMode, setViewMode] = useState<ViewMode>('all');
  const [isAuthModalOpen, setIsAuthModalOpen] = useState(false);
  const [userEmail, setUserEmail] = useState<string | null>(localStorage.getItem('user_email'));
  const [filters, setFilters] = useState<FilterValues>({ keyword: '', direction: '', minSalaryRub: '', grade: '' });

  const isAuthenticated = !!userEmail;

  const fetchData = async () => {
    // Не грузим вакансии заново, если открыт профиль
    if (viewMode === 'profile') return;

    setLoading(true);
    try {
      if (viewMode === 'favorites') {
        const data: Vacancy[] = await api.fetchWithAuth('/favorites');
        setVacancies(data);
        setFavoriteIds(new Set(data.map(v => v.id)));
      } else {
        // Для вкладки 'all' и 'stats' грузим общие данные
        const size = viewMode === 'stats' ? '100' : '10';

        // Собираем параметры аккуратно и РАЗДЕЛЬНО
        const queryParams = new URLSearchParams({
          page: viewMode === 'stats' ? '0' : currentPage.toString(),
          size: size,
        });

        if (filters.keyword) {
          queryParams.append('keyword', filters.keyword);
        }

        // Отправляем direction своим отдельным параметром!
        if (filters.direction && filters.direction !== 'Все направления') {
          queryParams.append('direction', filters.direction);
        }

        if (filters.minSalaryRub) {
          queryParams.append('minSalaryRub', filters.minSalaryRub);
        }

        const targetGrade = viewMode === 'internships' ? 'INTERN' : filters.grade;
        if (targetGrade && targetGrade !== 'Любой грейд') {
          queryParams.append('grade', targetGrade);
        }

        const response = await fetch(`http://localhost:8080/api/vacancies?${queryParams.toString()}`);
        const data: PageResponse<Vacancy> = await response.json();

        setVacancies(data.content);
        setTotalPages(data.totalPages);

        if (isAuthenticated) {
          const favs: Vacancy[] = await api.fetchWithAuth('/favorites');
          setFavoriteIds(new Set(favs.map(v => v.id)));
        }
      }
    } catch (err: any) {
      setError('Не удалось загрузить данные');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [viewMode, filters, currentPage, isAuthenticated]);

  const handleToggleFavorite = async (id: number) => {
    if (favoriteIds.has(id)) {
      await api.fetchWithAuth(`/favorites/${id}`, { method: 'DELETE' });
      setFavoriteIds(prev => { const n = new Set(prev); n.delete(id); return n; });
    } else {
      await api.fetchWithAuth(`/favorites/${id}`, { method: 'POST' });
      setFavoriteIds(prev => new Set(prev).add(id));
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 pb-20 font-sans">
      <nav className="bg-white/80 backdrop-blur-md sticky top-0 z-40 border-b border-gray-100 px-6 py-4 mb-8">
        <div className="max-w-5xl mx-auto flex justify-between items-center">
          <h1 className="text-2xl font-black text-gray-900 tracking-tighter cursor-pointer" onClick={() => setViewMode('all')}>
            DEV<span className="text-blue-600">HUNT</span>
          </h1>
          <div className="flex items-center gap-6">
            {isAuthenticated ? (
              <div className="flex items-center gap-4">
                <button
                  onClick={() => setViewMode('profile')}
                  className="flex items-center gap-2 text-sm font-bold text-gray-700 hover:text-blue-600 transition-colors"
                >
                  <div className="w-8 h-8 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center">
                    {userEmail?.charAt(0).toUpperCase()}
                  </div>
                  <span className="hidden sm:inline">{userEmail}</span>
                </button>
                <button onClick={() => { localStorage.clear(); window.location.reload(); }} className="text-sm text-gray-400 hover:text-red-500 font-bold transition-colors">Выйти</button>
              </div>
            ) : (
              <button onClick={() => setIsAuthModalOpen(true)} className="bg-blue-600 text-white px-6 py-2 rounded-xl font-bold hover:bg-blue-700 transition-all shadow-md shadow-blue-200">Войти</button>
            )}
          </div>
        </div>
      </nav>

      <main className="max-w-4xl mx-auto px-4">
        {/* Навигация по вкладкам */}
        <div className="flex flex-wrap gap-6 mb-8 border-b border-gray-200">
          <button onClick={() => { setViewMode('all'); setCurrentPage(0); }} className={`text-lg font-bold pb-3 transition-all ${viewMode === 'all' ? 'text-blue-600 border-b-4 border-blue-600' : 'text-gray-400 hover:text-gray-600'}`}>Лента вакансий</button>
          {/* НОВАЯ ВКЛАДКА */}
          <button onClick={() => { setViewMode('internships'); setCurrentPage(0); }} className={`text-lg font-bold pb-3 transition-all ${viewMode === 'internships' ? 'text-blue-600 border-b-4 border-blue-600' : 'text-gray-400 hover:text-gray-600'}`}>Стажировки</button>
          <button onClick={() => setViewMode('stats')} className={`text-lg font-bold pb-3 transition-all ${viewMode === 'stats' ? 'text-blue-600 border-b-4 border-blue-600' : 'text-gray-400 hover:text-gray-600'}`}>Аналитика</button>
          {isAuthenticated && (
            <>
              <button onClick={() => setViewMode('favorites')} className={`text-lg font-bold pb-3 transition-all ${viewMode === 'favorites' ? 'text-blue-600 border-b-4 border-blue-600' : 'text-gray-400 hover:text-gray-600'}`}>Избранное ({favoriteIds.size})</button>
              <button onClick={() => setViewMode('profile')} className={`text-lg font-bold pb-3 transition-all ${viewMode === 'profile' ? 'text-blue-600 border-b-4 border-blue-600' : 'text-gray-400 hover:text-gray-600'} sm:hidden`}>Профиль</button>
            </>
          )}
        </div>

        {/* Контент в зависимости от вкладки */}
        {viewMode === 'profile' && isAuthenticated && <ProfilePanel />}

        {viewMode === 'stats' && <StatsDashboard vacancies={vacancies} />}

        {(viewMode === 'all' || viewMode === 'favorites' || viewMode === 'internships') && (
          <>
            {/* Показываем фильтры и для Всех вакансий, и для Стажировок */}
            {(viewMode === 'all' || viewMode === 'internships') && (
              <FilterPanel
                onFilterChange={(f) => { setFilters(f); setCurrentPage(0); }}
                hideGrade={viewMode === 'internships'} // Прячем выбор грейда, если мы в стажировках
              />
            )}

            {loading ? (
              <div className="flex justify-center my-20"><div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div></div>
            ) : error ? (
              <div className="bg-red-50 text-red-600 p-4 rounded-lg text-center font-medium mb-6">{error}</div>
            ) : vacancies.length === 0 ? (
              <div className="text-center text-gray-500 my-20 bg-white p-10 rounded-xl border border-gray-100">Ничего не найдено 📭</div>
            ) : (
              <div className="space-y-4">
                {vacancies.map(v => (
                  <div key={v.id} onClick={() => setSelectedVacancy(v)} className="cursor-pointer">
                    <VacancyCard vacancy={v} isAuthenticated={isAuthenticated} isFavorite={favoriteIds.has(v.id)} onToggleFavorite={(id) => handleToggleFavorite(id)} />
                  </div>
                ))}
              </div>
            )}

            {/* Пагинация */}
            {(viewMode === 'all' || viewMode === 'internships') && totalPages > 1 && (() => {
              // Генерируем номера страниц с многоточиями
              const pages: (number | '...')[] = [];
              const delta = 2; // сколько страниц показывать вокруг текущей

              // Всегда показываем первую страницу
              pages.push(0);

              const rangeStart = Math.max(1, currentPage - delta);
              const rangeEnd = Math.min(totalPages - 2, currentPage + delta);

              if (rangeStart > 1) pages.push('...');
              for (let i = rangeStart; i <= rangeEnd; i++) pages.push(i);
              if (rangeEnd < totalPages - 2) pages.push('...');

              // Всегда показываем последнюю страницу (если больше 1)
              if (totalPages > 1) pages.push(totalPages - 1);

              return (
                <div className="flex justify-center items-center gap-1.5 mt-12 flex-wrap">
                  {/* В начало */}
                  <button
                    disabled={currentPage === 0}
                    onClick={() => setCurrentPage(0)}
                    className="px-3 py-2 bg-white border border-gray-200 rounded-xl font-bold disabled:opacity-30 hover:bg-blue-50 hover:border-blue-300 transition-all text-sm"
                    title="В начало"
                  >
                    «
                  </button>
                  {/* Назад */}
                  <button
                    disabled={currentPage === 0}
                    onClick={() => setCurrentPage(p => p - 1)}
                    className="px-3 py-2 bg-white border border-gray-200 rounded-xl font-bold disabled:opacity-30 hover:bg-blue-50 hover:border-blue-300 transition-all text-sm"
                    title="Назад"
                  >
                    ‹
                  </button>

                  {/* Номера страниц */}
                  {pages.map((p, idx) =>
                    p === '...' ? (
                      <span key={`dots-${idx}`} className="px-2 py-2 text-gray-400 font-bold select-none">…</span>
                    ) : (
                      <button
                        key={p}
                        onClick={() => setCurrentPage(p)}
                        className={`min-w-[40px] px-3 py-2 rounded-xl font-bold text-sm transition-all ${p === currentPage
                          ? 'bg-blue-600 text-white shadow-md shadow-blue-200 border border-blue-600'
                          : 'bg-white border border-gray-200 hover:bg-blue-50 hover:border-blue-300 text-gray-700'
                          }`}
                      >
                        {p + 1}
                      </button>
                    )
                  )}

                  {/* Вперёд */}
                  <button
                    disabled={currentPage === totalPages - 1}
                    onClick={() => setCurrentPage(p => p + 1)}
                    className="px-3 py-2 bg-white border border-gray-200 rounded-xl font-bold disabled:opacity-30 hover:bg-blue-50 hover:border-blue-300 transition-all text-sm"
                    title="Вперёд"
                  >
                    ›
                  </button>
                  {/* В конец */}
                  <button
                    disabled={currentPage === totalPages - 1}
                    onClick={() => setCurrentPage(totalPages - 1)}
                    className="px-3 py-2 bg-white border border-gray-200 rounded-xl font-bold disabled:opacity-30 hover:bg-blue-50 hover:border-blue-300 transition-all text-sm"
                    title="В конец"
                  >
                    »
                  </button>

                  {/* Текст-подсказка */}
                  <span className="ml-3 text-sm text-gray-400 font-medium hidden sm:inline">
                    Страница {currentPage + 1} из {totalPages}
                  </span>
                </div>
              );
            })()}
          </>
        )}
      </main>

      <AuthModal isOpen={isAuthModalOpen} onClose={() => setIsAuthModalOpen(false)} onLoginSuccess={(t, e) => { localStorage.setItem('jwt_token', t); localStorage.setItem('user_email', e); setUserEmail(e); }} />
      <VacancyModal vacancy={selectedVacancy} onClose={() => setSelectedVacancy(null)} />
    </div>
  );
}

export default App;