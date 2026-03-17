CREATE TABLE IF NOT EXISTS cached_games (
    igdb_id          BIGINT PRIMARY KEY,
    name             TEXT NOT NULL,
    summary          TEXT,
    rating           DOUBLE PRECISION,
    rating_count     INTEGER,
    first_release    DATE,
    cover_url        TEXT,
    genre_ids        INTEGER[],
    theme_ids        INTEGER[],
    platform_ids     INTEGER[],
    similar_game_ids BIGINT[],
    keyword_ids      INTEGER[],
    game_mode_ids    INTEGER[],
    perspective_ids  INTEGER[],
    developer_slugs  TEXT[],
    franchise_ids    BIGINT[],
    raw_json         TEXT,
    cached_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS genres (
    igdb_id INTEGER PRIMARY KEY,
    name    TEXT NOT NULL,
    slug    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS themes (
    igdb_id INTEGER PRIMARY KEY,
    name    TEXT NOT NULL,
    slug    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS platforms (
    igdb_id  INTEGER PRIMARY KEY,
    name     TEXT NOT NULL,
    slug     TEXT NOT NULL,
    category INTEGER
);

CREATE TABLE IF NOT EXISTS game_modes (
    igdb_id INTEGER PRIMARY KEY,
    name    TEXT NOT NULL,
    slug    TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS player_perspectives (
    igdb_id INTEGER PRIMARY KEY,
    name    TEXT NOT NULL,
    slug    TEXT NOT NULL
);
