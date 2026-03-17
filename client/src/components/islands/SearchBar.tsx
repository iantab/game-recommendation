import { useState, useEffect, useRef } from 'preact/hooks';
import type { GameDto } from '../../lib/types';
import { searchGames } from '../../lib/api';
import {
  isLiked,
  toggleLikedGame,
  onLikedGamesChange,
  offLikedGamesChange,
} from '../../lib/likedGames';

export default function SearchBar() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<GameDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [, setLikedVersion] = useState(0);
  const timerRef = useRef<ReturnType<typeof setTimeout>>(null);

  useEffect(() => {
    const handler = () => setLikedVersion((v) => v + 1);
    onLikedGamesChange(handler);
    return () => offLikedGamesChange(handler);
  }, []);

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
          {results.map((game) => {
            const liked = isLiked(game.igdbId);
            return (
              <div class="game-card" key={game.igdbId}>
                <div class="game-card__cover-wrap">
                  {game.coverUrl ? (
                    <img
                      class="game-card__cover"
                      src={game.coverUrl}
                      alt={game.name}
                      loading="lazy"
                    />
                  ) : (
                    <div class="game-card__cover--placeholder">?</div>
                  )}
                  <button
                    class={`game-card__like ${liked ? 'game-card__like--active' : ''}`}
                    onClick={() =>
                      toggleLikedGame({
                        igdbId: game.igdbId,
                        name: game.name,
                        coverUrl: game.coverUrl,
                      })
                    }
                    aria-label={liked ? `Unlike ${game.name}` : `Like ${game.name}`}
                  >
                    {liked ? '\u2665' : '\u2661'}
                  </button>
                </div>
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
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
