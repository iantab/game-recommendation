import { useState, useEffect } from 'preact/hooks';
import type { GameDto } from '../../lib/types';

export default function GameGrid() {
  const [games, setGames] = useState<GameDto[]>([]);

  useEffect(() => {
    const handler = (e: Event) => {
      setGames((e as CustomEvent<GameDto[]>).detail);
    };
    window.addEventListener('gamerec:recommendations', handler);
    return () => window.removeEventListener('gamerec:recommendations', handler);
  }, []);

  if (games.length === 0) return null;

  return (
    <div style={{ marginTop: '2rem' }}>
      <h2 class="section__title">Your Recommendations</h2>
      <p class="section__subtitle">{games.length} games matched your preferences</p>
      <div class="game-grid">
        {games.map((game) => (
          <div class="game-card" key={game.igdbId}>
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
                {game.score != null && (
                  <span class="game-card__score">{(game.score * 100).toFixed(0)}% match</span>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
