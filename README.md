# Game Recommendation Engine

A game recommendation engine powered by IGDB data. Select your favorite genres, themes, and platforms to get personalized game suggestions.

## Tech Stack

### Backend

- Java 21, Spring Boot 4, Spring Data JPA
- PostgreSQL 17
- AWS Lambda (SAM + `aws-serverless-java-container-springboot4`)
- IGDB API integration (via Twitch/IGDB)

### Frontend

- Astro 6, Preact, TypeScript
- Bun

## Project Structure

```
game-recommendation/
├── client/             # Astro frontend
├── server/             # Spring Boot backend
│   └── template.yaml   # SAM deployment template
└── docker-compose.yml  # Local PostgreSQL
```

## Local Development

### Prerequisites

- Java 21
- Bun
- Docker
- IGDB credentials (register at the [Twitch Developer Console](https://dev.twitch.tv/console) to get a Client ID and Client Secret)

### Start the database

```sh
docker compose up -d
```

### Set environment variables

```sh
export IGDB_CLIENT_ID=your_client_id
export IGDB_CLIENT_SECRET=your_client_secret
```

### Run the backend

```sh
cd server
./gradlew bootRun
```

Runs on port 8080 using the `local` Spring profile.

### Run the frontend

```sh
cd client
bun install
bun run dev
```

Runs on port 4321 and proxies `/api` requests to the backend.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/games/search` | Search games by query |
| `GET` | `/api/games/{id}` | Get a game by ID |
| `POST` | `/api/recommendations` | Get personalized recommendations |
| `GET` | `/api/genres` | List all genres |
| `GET` | `/api/themes` | List all themes |
| `GET` | `/api/platforms` | List all platforms |

## Deployment

The backend deploys as an AWS Lambda function via the SAM template at `server/template.yaml`. Java 21 SnapStart is enabled for fast cold starts. The frontend is built as a static site.
