import { useState, useEffect, useRef } from 'preact/hooks';
import type { GameDto } from '../../lib/types';
import { searchGames } from '../../lib/api';

export default function SearchBar() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<GameDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  useEffect(() => {
    if (query.length < 2) {
      setResults([]);
      setSearched(false);
      return;
    }

    if (timerRef.current) clearTimeout(timerRef.current);

    timerRef.current = setTimeout(async () => {
      setLoading(true);
      try {
        const games = await searchGames(query);
        setResults(games);
      } catch {
        setResults([]);
      }
      setSearched(true);
      setLoading(false);
    }, 300);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [query]);

  return (
    <div>
      <input
        type="text"
        class="search-input"
        placeholder="Search for games... (e.g. Zelda, Elden Ring)"
        value={query}
        onInput={(e) => setQuery((e.target as HTMLInputElement).value)}
        autofocus
      />

      {loading && <p class="loading">Searching...</p>}

      {!loading && searched && results.length === 0 && (
        <p class="empty">No games found for "{query}"</p>
      )}

      {results.length > 0 && (
        <div class="game-grid">
          {results.map((game) => (
            <a class="game-card" href={`/game?id=${game.igdbId}`} key={game.igdbId}>
              {game.coverUrl ? (
                <img class="game-card__cover" src={game.coverUrl} alt={game.name} loading="lazy" />
              ) : (
                <div class="game-card__cover--placeholder">?</div>
              )}
              <div class="game-card__body">
                <div class="game-card__name">{game.name}</div>
                <div class="game-card__meta">
                  {game.rating != null && (
                    <span
                      class={`game-card__rating ${
                        game.rating >= 80
                          ? 'game-card__rating--high'
                          : game.rating >= 60
                            ? 'game-card__rating--mid'
                            : 'game-card__rating--low'
                      }`}
                    >
                      {Math.round(game.rating)}
                    </span>
                  )}
                  {game.genres.length > 0 && (
                    <span style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                      {game.genres.map((g) => g.name).join(', ')}
                    </span>
                  )}
                </div>
              </div>
            </a>
          ))}
        </div>
      )}
    </div>
  );
}
