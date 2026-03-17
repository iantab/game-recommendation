export interface GameDto {
  igdbId: number;
  name: string;
  summary: string | null;
  rating: number | null;
  ratingCount: number | null;
  firstRelease: string | null;
  coverUrl: string | null;
  genres: GenreDto[];
  themes: ThemeDto[];
  platforms: PlatformDto[];
  score: number | null;
}

export interface GenreDto {
  id: number;
  name: string;
  slug: string;
}

export interface ThemeDto {
  id: number;
  name: string;
  slug: string;
}

export interface PlatformDto {
  id: number;
  name: string;
  slug: string;
  category: number | null;
}

export interface SearchRequest {
  query: string;
  genreIds?: number[];
  platformIds?: number[];
  limit?: number;
}

export interface PreferenceRequest {
  genreIds: number[];
  themeIds: number[];
  platformIds: number[];
  likedGameIds?: number[];
}
