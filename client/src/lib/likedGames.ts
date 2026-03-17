export interface LikedGame {
  igdbId: number;
  name: string;
  coverUrl: string | null;
}

const STORAGE_KEY = 'gamerec:liked';
const CHANGE_EVENT = 'gamerec:liked-changed';

function read(): LikedGame[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
  } catch {
    return [];
  }
}

function write(games: LikedGame[]) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(games));
  window.dispatchEvent(new Event(CHANGE_EVENT));
}

export function getLikedGames(): LikedGame[] {
  return read();
}

export function isLiked(igdbId: number): boolean {
  return read().some((g) => g.igdbId === igdbId);
}

export function addLikedGame(game: LikedGame) {
  const games = read();
  if (!games.some((g) => g.igdbId === game.igdbId)) {
    games.push(game);
    write(games);
  }
}

export function removeLikedGame(igdbId: number) {
  write(read().filter((g) => g.igdbId !== igdbId));
}

export function toggleLikedGame(game: LikedGame) {
  if (isLiked(game.igdbId)) {
    removeLikedGame(game.igdbId);
  } else {
    addLikedGame(game);
  }
}

export function onLikedGamesChange(cb: () => void) {
  window.addEventListener(CHANGE_EVENT, cb);
  window.addEventListener('storage', (e) => {
    if (e.key === STORAGE_KEY) cb();
  });
}

export function offLikedGamesChange(cb: () => void) {
  window.removeEventListener(CHANGE_EVENT, cb);
}
