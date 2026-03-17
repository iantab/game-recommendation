import { useState, useEffect } from 'preact/hooks';
import type { GenreDto, ThemeDto, PlatformDto } from '../../lib/types';
import { getGenres, getThemes, getPlatforms, getRecommendations } from '../../lib/api';
import { getLikedGames, removeLikedGame, onLikedGamesChange, offLikedGamesChange } from '../../lib/likedGames';
import type { LikedGame } from '../../lib/likedGames';

export default function PreferencePicker() {
  const [genres, setGenres] = useState<GenreDto[]>([]);
  const [themes, setThemes] = useState<ThemeDto[]>([]);
  const [platforms, setPlatforms] = useState<PlatformDto[]>([]);
  const [likedGames, setLikedGames] = useState<LikedGame[]>([]);

  const [selectedGenres, setSelectedGenres] = useState<Set<number>>(new Set());
  const [selectedThemes, setSelectedThemes] = useState<Set<number>>(new Set());
  const [selectedPlatforms, setSelectedPlatforms] = useState<Set<number>>(new Set());

  const [loadingData, setLoadingData] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([getGenres(), getThemes(), getPlatforms()])
      .then(([g, t, p]) => {
        setGenres(g);
        setThemes(t);
        setPlatforms(p);
      })
      .finally(() => setLoadingData(false));
  }, []);

  useEffect(() => {
    setLikedGames(getLikedGames());
    const handler = () => setLikedGames(getLikedGames());
    onLikedGamesChange(handler);
    return () => offLikedGamesChange(handler);
  }, []);

  const toggle = (set: Set<number>, id: number, setter: (s: Set<number>) => void) => {
    const next = new Set(set);
    if (next.has(id)) next.delete(id);
    else next.add(id);
    setter(next);
  };

  const handleSubmit = async () => {
    if (selectedGenres.size === 0 && selectedThemes.size === 0 && likedGames.length === 0) return;
    setSubmitting(true);
    try {
      const results = await getRecommendations({
        genreIds: [...selectedGenres],
        themeIds: [...selectedThemes],
        platformIds: [...selectedPlatforms],
        likedGameIds: likedGames.map((g) => g.igdbId),
      });
      window.dispatchEvent(new CustomEvent('gamerec:recommendations', { detail: results }));
    } catch (e) {
      console.error('Failed to get recommendations', e);
    }
    setSubmitting(false);
  };

  if (loadingData) return <p class="loading">Loading preferences...</p>;

  const hasSelection = selectedGenres.size > 0 || selectedThemes.size > 0 || likedGames.length > 0;

  return (
    <div>
      {likedGames.length > 0 && (
        <div class="picker__section">
          <label class="picker__label">
            Liked games ({likedGames.length})
          </label>
          <div class="liked-games">
            {likedGames.map((game) => (
              <div class="liked-games__chip" key={game.igdbId}>
                {game.coverUrl && (
                  <img class="liked-games__cover" src={game.coverUrl} alt="" />
                )}
                <span class="liked-games__name">{game.name}</span>
                <button
                  class="liked-games__remove"
                  onClick={() => removeLikedGame(game.igdbId)}
                  aria-label={`Remove ${game.name}`}
                >
                  &times;
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      <div class="picker__section">
        <label class="picker__label">
          Genres you enjoy ({selectedGenres.size} selected)
        </label>
        <div class="tags">
          {genres.map((g) => (
            <button
              key={g.id}
              class={`tag ${selectedGenres.has(g.id) ? 'tag--selected' : ''}`}
              onClick={() => toggle(selectedGenres, g.id, setSelectedGenres)}
            >
              {g.name}
            </button>
          ))}
        </div>
      </div>

      <div class="picker__section">
        <label class="picker__label">
          Themes you like ({selectedThemes.size} selected)
        </label>
        <div class="tags">
          {themes.map((t) => (
            <button
              key={t.id}
              class={`tag ${selectedThemes.has(t.id) ? 'tag--selected' : ''}`}
              onClick={() => toggle(selectedThemes, t.id, setSelectedThemes)}
            >
              {t.name}
            </button>
          ))}
        </div>
      </div>

      <div class="picker__section">
        <label class="picker__label">
          Platforms you own ({selectedPlatforms.size} selected)
        </label>
        <div class="tags">
          {platforms
            .filter((p) => p.category != null && p.category <= 6)
            .map((p) => (
              <button
                key={p.id}
                class={`tag ${selectedPlatforms.has(p.id) ? 'tag--selected' : ''}`}
                onClick={() => toggle(selectedPlatforms, p.id, setSelectedPlatforms)}
              >
                {p.name}
              </button>
            ))}
        </div>
      </div>

      <button
        class="btn btn--primary"
        onClick={handleSubmit}
        disabled={!hasSelection || submitting}
      >
        {submitting ? 'Finding games...' : 'Get Recommendations'}
      </button>
    </div>
  );
}
