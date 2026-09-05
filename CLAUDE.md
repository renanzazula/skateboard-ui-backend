# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A thin Spring Boot Backend-for-Frontend (`ui-backend`, package `com.skateboard.uibackend`) sitting between the Skateboard frontend and internal microservices. It validates the frontend's Keycloak JWT, relays it downstream unmodified (token relay — this service has no Keycloak client of its own), and exposes a stable frontend-facing API. Full design rationale: `.docs/README-skateboard-ui-backend.md`.

Four downstream services exist: `../skateboard-podcast-be`, `../skateboard-user-be`, `../skateboard-app-config-be` and `../skateboard-notification-be`, each with its own vendored spec under `api/`, its own generator execution in `pom.xml`, and its own `client/<name>/` wrapper. Events/spots services from the design doc don't exist, so there's no client or controller for them — don't add one speculatively; follow the `client/podcast/` pattern once a real service exists to call.

`/api/me/preferences` is served by `NotificationController` against `skateboard-notification-be`, not by `UserController` against `skateboard-user-be`, which owned notification preferences until that service existed. The route, the DTO shape and the `FUNC_USER_SELF_READ`/`FUNC_USER_SELF_UPDATE` authorities are all unchanged — only what stands behind them moved — which is why the mobile settings screen needed no change. `skateboard-user-be` still exposes its own copy; nothing calls it.

`HomeController` (`/api/home/**`) is the one place this BFF actually aggregates instead of passing through: see **Home dashboard aggregation** below.

## Build & run

- `mvn package` (or the committed wrapper: `./mvnw package` / `mvnw.cmd package` — this is what the deploy build uses, see Deployment below) — runs the `openapi-generator-maven-plugin` step first (see below), then compiles and runs tests.
- `mvn spring-boot:run` — needs a reachable Keycloak (`skateboard-podcast-be/.docker/docker-compose.yaml`, realm `skateboard-podcast`, `localhost:8180`) and whichever downstream service(s) the routes you're exercising call: `skateboard-podcast-be` (`:8080`), `skateboard-user-be` (`:8082`), `skateboard-app-config-be` (`:8083`), `skateboard-notification-be` (`:8084`) — see `clients.*.base-url` in `application.yml` for the full list/defaults. Runs on `:8090` (`8081` is Expo/Metro, `8180` Keycloak). No database of its own — this service is stateless.
- `mvn test` — the surefire plugin passes `-Dnet.bytebuddy.experimental=true`; without it, `@MockBean`-based tests fail to self-attach Mockito's inline mock maker on JDKs newer than Byte Buddy officially supports (JDK 25 on this machine vs. the project's Java 21 target). Safe/inert on JDKs Byte Buddy does support — don't remove it just because your local JDK doesn't need it.

## Generated OpenAPI client

`api/openapi.yaml`, `api/user-openapi.yaml`, `api/app-config-openapi.yaml` and `api/notification-openapi.yaml` are **vendored copies** of the downstream services' specs — this repo owns none of those contracts, and there's no shared contract registry. When an upstream spec changes, re-copy it here by hand, and check whether the matching controller's `@PreAuthorize` authorities still match its `x-required-permissions`. `api/bff-openapi.yaml` is this service's *own* exposed contract, hand-synced, and is what the frontend generates its types from.

`pom.xml`'s `openapi-generator-maven-plugin` generates a WebClient-based client (`generatorName=java`, `library=webclient`) from that spec into `com.skateboard.uibackend.client.podcast.generated.{api,model,invoker}` at build time — `PodcastApi`, `ApiClient`, and the request/response DTOs (`PostResponse`, `FeedPageResponse`, `CreatePostRequest`, etc.). These are regenerated on every build; don't hand-edit anything under `client/podcast/generated`.

Two dependencies exist solely to make the generated code compile under Spring Boot 3's Jakarta namespace: `javax.annotation:javax.annotation-api` (the templates emit `@javax.annotation.Generated`) and `com.google.code.findbugs:jsr305` (emits `javax.annotation.Nullable`). Not needed by hand-written code — don't reference `javax.annotation.*` in new code, use `jakarta.*`/framework equivalents instead.

## Request flow

```
PodcastController  (@PreAuthorize, mirrors x-required-permissions from the vendored spec)
      |
      v
PodcastService      (thin pass-through today; the seam for future aggregation)
      |
      v
PodcastClient       (blocks on the generated client's Mono; maps failures)
      |
      v
PodcastApi (generated) -- WebClient --> skateboard-podcast-be
```

- **Auth** (`config/SecurityConfig`): OAuth2 resource server, JWKS URI built directly from `issuer-uri` (`{issuer}/protocol/openid-connect/certs`) rather than OIDC discovery, so boot doesn't block on Keycloak being reachable at that exact moment — same reasoning as `skateboard-podcast-be`'s `SecurityConfig`. Authorities come verbatim (no `ROLE_`/`SCOPE_` prefix) from the JWT's `authorities` claim. No `AudienceValidator` here (unlike podcast-be): the relayed token's `aud` is validated by whichever downstream service receives it, and there's no Keycloak client/audience registered for this BFF itself.
- **Token relay** (`web/BearerTokenExchangeFilter`): reads the inbound `JwtAuthenticationToken` off `SecurityContextHolder` and adds it as `Authorization: Bearer …` on outgoing WebClient calls. This only works because `PodcastClient` blocks synchronously on the same thread handling the inbound request — if a client is ever made non-blocking/async, this filter needs Reactor context propagation instead of ThreadLocal.
- **Correlation IDs** (`web/CorrelationIdFilter`, `web/CorrelationIdExchangeFilter`): `CorrelationIdFilter` runs at `Ordered.HIGHEST_PRECEDENCE` — ahead of Spring Security's filter chain — so a correlation id is in MDC even for requests Spring Security rejects before they reach a controller (see `RestAuthenticationEntryPoint`). `CorrelationIdExchangeFilter` re-reads it from MDC per downstream call, again relying on synchronous/same-thread execution.
- **Authorization boundary**: `@PreAuthorize` authorities on `PodcastController` are a deliberate, coarse-grained duplication of `x-required-permissions` from the vendored spec — an API-boundary check on token claims, not domain logic, so it doesn't conflict with keeping business rules downstream. Fine-grained/domain authorization still lives in `skateboard-podcast-be`. This is per-route, not per-controller: `AboutUsController`'s `GET /api/about-us` (the standard-user viewer) is gated by the baseline `FUNC_TAB_SETTINGS` while its other three routes require the admin-only `FUNC_ABOUT_US_MANAGE` — check each route's own spec entry, don't assume every method on a controller shares one authority.
- **Error shape** (`exception/GlobalExceptionHandler`, `exception/DownstreamServiceException`, `exception/ErrorResponse`): every error the frontend sees — from a controller, from `@PreAuthorize`, or from a rejected-before-dispatch auth failure (`web/RestAuthenticationEntryPoint`) — comes back as the same `{code, message, correlationId, timestamp}` shape. `PodcastClient` maps downstream 5xx/connectivity failures to `PODCAST_SERVICE_UNAVAILABLE` (503); downstream 4xx is passed through with its original status (that's the downstream service's own validation/not-found result, not an outage). A `null` service return generally means "downstream answered 204 / nothing to show" and is relayed as `204` (see `AboutUsController`, `HomeFeaturedPlayerService`) — not an error.

## Home dashboard aggregation

`HomeController` → `HomeService` / `HomeFeaturedPlayerService` is the one feature that isn't a thin pass-through — it's the aggregation `PodcastService`'s doc comment calls out as a future seam, now real:

- `HomeService.getVideos()` (`GET /api/home/videos`) reads the eligible-category config from `AppConfigClient` (`skateboard-app-config-be`) and merges in matching posts from `PodcastClient` (`skateboard-podcast-be`) — paging through the podcast feed or each selected category and de-duplicating by post id, since podcast-be has no bulk/multi-category endpoint. If app-config-be is unreachable it falls back to "ALL categories" rather than blanking the dashboard; a category slug the config references but podcast-be no longer has is skipped, not fatal.
- `HomeFeaturedPlayerService.getFeaturedPlayer()` (`GET /api/home/featured-player`) reads the Featured Player config from app-config-be, then resolves the configured `contentSource`/`contentId` via a `FeaturedContentResolver` (one implementation per `FeaturedContentSource` — today just `PodcastFeaturedContentResolver`, registered as a `@Component` and injected as `List<FeaturedContentResolver>`). Add a new source by adding a new resolver, not by branching inside the service. Any failure to resolve (disabled, unconfigured, downstream unavailable, referenced content deleted/unpublished) returns `null` → controller relays `204`, never an error — the rest of Home must stay usable regardless of this feature's state.

Both services are read-only aggregations; the corresponding *admin* config endpoints (`/api/config/home/video-categories`, `/api/config/home/featured-player`) are plain pass-throughs on `AppConfigController`, same pattern as everything else.

## Deployment

Deploys via Railway using `railpack.json` (provider `java`): the build runs `./mvnw clean package -DskipTests`, then downloads the New Relic Java agent and stages `newrelic-config/newrelic.yml` next to it. The start command conditionally adds `-javaagent:newrelic/newrelic.jar` based on the `NEW_RELIC_ENABLED` env var — the jar/config are always staged, but the agent only attaches when that flag is `true`. `application-railway.yml` is the Railway-profile override of `application.yml` (same shape, same defaults — check both when changing config). `/actuator/health` (`spring-boot-starter-actuator`) is the one endpoint `SecurityConfig` permits without a token, presumably for Railway's health check.

## Tests

JUnit 5 (+ AssertJ, spring-security-test), in two shapes:
- Plain unit tests with no Spring context — `exception/GlobalExceptionHandlerTest` (exception → `ErrorResponse` mapping), and the `service/` tests, which use `@Mock` + `MockitoAnnotations.openMocks` and construct the service by hand.
- `controller/*SecurityTest` — `@WebMvcTest` + `@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})`, verifying this BFF's own auth gate (no token → 401 `UNAUTHENTICATED`, wrong authority → 403 `FORBIDDEN`, correct authority → 2xx) with the service mocked out. These do **not** test downstream behaviour — that's each `client/`'s and, further down, each service's own suite's job.
