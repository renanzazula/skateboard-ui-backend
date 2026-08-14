# skateboard-ui-backend

Backend for Frontend (BFF) for the Skateboard frontend. Validates the Keycloak JWT the frontend sends, relays it to internal microservices, and exposes a stable, UI-oriented API. See [`.docs/README-skateboard-ui-backend.md`](.docs/README-skateboard-ui-backend.md) for the full design.

Currently wired up: `skateboard-podcast-be`, passed through at `/api/podcast/**` and
`/api/admin/podcast/**`, and `skateboard-user-be`, passed through at `/api/me/**`.

## API contracts

Three separate OpenAPI files, three separate purposes:

| File | Role |
|---|---|
| `api/openapi.yaml` | Vendored copy of `skateboard-podcast-be`'s own spec — codegen input for the outbound `PodcastApi` client only. |
| `api/user-openapi.yaml` | Vendored copy of `skateboard-user-be`'s own spec — codegen input for the outbound `MeApi` client only. |
| `api/bff-openapi.yaml` | **This service's own exposed contract** (`/api/podcast/**` + `/api/me/**`, as implemented by `PodcastController`/`UserController`) — not wired into any codegen here; it exists for external consumers (`skateboard-fe`) to vendor and generate their own typed client from. Keep it in sync by hand when a controller's routes/DTOs/`@PreAuthorize` authorities change. |

## Build

```
mvn package
```

Runs `openapi-generator-maven-plugin` first (generates the `PodcastApi` WebClient client from
`api/openapi.yaml` and the `MeApi` WebClient client from `api/user-openapi.yaml`), then compiles
and runs the test suite.

## Run

Needs a reachable Keycloak, a reachable `skateboard-podcast-be`, and a reachable `skateboard-user-be`:

```
# in ../skateboard-podcast-be
docker compose -f .docker/docker-compose.yaml up -d skateboard-keycloak skateboard-keycloak-postgres
mvn spring-boot:run   # starts skateboard-podcast-be on :8080

# in ../skateboard-user-be
mvn spring-boot:run   # starts skateboard-user-be on :8082

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
| `CLIENTS_USER_BASE_URL` | `http://localhost:8082` |
| `CLIENTS_USER_CONNECT_TIMEOUT_MS` | `3000` |
| `CLIENTS_USER_READ_TIMEOUT_MS` | `5000` |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8081` |

## Test

```
mvn test
```
