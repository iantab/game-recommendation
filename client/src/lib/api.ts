import type { GameDto, GenreDto, ThemeDto, PlatformDto, PreferenceRequest } from './types';

const API_BASE = import.meta.env.PUBLIC_API_URL || '';

export async function searchGames(query: string, limit = 20): Promise<GameDto[]> {
  const res = await fetch(`${API_BASE}/api/games/search`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, limit }),
  });
  if (!res.ok) throw new Error(`Search failed: ${res.status}`);
  return res.json();
}

export async function getGame(id: number): Promise<GameDto> {
  const res = await fetch(`${API_BASE}/api/games/${id}`);
  if (!res.ok) throw new Error(`Game fetch failed: ${res.status}`);
  return res.json();
}

export async function getRecommendations(prefs: PreferenceRequest): Promise<GameDto[]> {
  const res = await fetch(`${API_BASE}/api/recommendations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(prefs),
  });
  if (!res.ok) throw new Error(`Recommendations failed: ${res.status}`);
  return res.json();
}

export async function getGenres(): Promise<GenreDto[]> {
  const res = await fetch(`${API_BASE}/api/genres`);
  if (!res.ok) throw new Error(`Genres fetch failed: ${res.status}`);
  return res.json();
}

export async function getThemes(): Promise<ThemeDto[]> {
  const res = await fetch(`${API_BASE}/api/themes`);
  if (!res.ok) throw new Error(`Themes fetch failed: ${res.status}`);
  return res.json();
}

export async function getPlatforms(): Promise<PlatformDto[]> {
  const res = await fetch(`${API_BASE}/api/platforms`);
  if (!res.ok) throw new Error(`Platforms fetch failed: ${res.status}`);
  return res.json();
}
