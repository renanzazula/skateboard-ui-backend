# skateboard-ui-backend

Backend for Frontend (BFF) for the Skateboard frontend. Validates the Keycloak JWT the frontend sends, relays it to internal microservices, and exposes a stable, UI-oriented API. See [`.docs/README-skateboard-ui-backend.md`](.docs/README-skateboard-ui-backend.md) for the full design.

Currently wired up: `skateboard-podcast-be` (the only downstream service that exists yet), passed through at `/api/podcast/**` and `/api/admin/podcast/**`.

## Build

```
mvn package
```

Runs `openapi-generator-maven-plugin` first (generates the `PodcastApi` WebClient client from `api/podcast-openapi.yaml`), then compiles and runs the test suite.

## Run

Needs a reachable Keycloak and a reachable `skateboard-podcast-be`:

```
# in ../skateboard-podcast-be
docker compose -f .docker/docker-compose.yaml up -d skateboard-keycloak skateboard-keycloak-postgres
mvn spring-boot:run   # starts skateboard-podcast-be on :8080

# in this repo
mvn spring-boot:run   # starts ui-backend on :8090
```

Config (env vars, all optional — defaults match the setup above):

| Variable | Default |
|---|---|
| `APP_SECURITY_OAUTH2_ISSUER_URI` | `http://localhost:8180/realms/skateboard-podcast` |
| `CLIENTS_PODCAST_BASE_URL` | `http://localhost:8080` |
| `CLIENTS_PODCAST_CONNECT_TIMEOUT_MS` | `3000` |
| `CLIENTS_PODCAST_READ_TIMEOUT_MS` | `5000` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8081` |

## Test

```
mvn test
```
