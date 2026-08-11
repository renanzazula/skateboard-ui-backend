# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A thin Spring Boot Backend-for-Frontend (`ui-backend`, package `com.skateboard.uibackend`) sitting between the Skateboard frontend and internal microservices. It validates the frontend's Keycloak JWT, relays it downstream unmodified (token relay — this service has no Keycloak client of its own), and exposes a stable frontend-facing API. Full design rationale: `.docs/README-skateboard-ui-backend.md`.

Only one downstream service exists yet: `../skateboard-podcast-be` (sibling repo). Events/spots services from the design doc don't exist, so there's no client, controller, or `/api/home` aggregation for them — don't add one speculatively; follow the `client/podcast/` pattern once a real service exists to call.

## Build & run

- `mvn package` — runs the `openapi-generator-maven-plugin` step first (see below), then compiles and runs tests.
- `mvn spring-boot:run` — needs a reachable Keycloak (`skateboard-podcast-be/.docker/docker-compose.yaml`, realm `skateboard-podcast`, `localhost:8180`) and a reachable `skateboard-podcast-be` (`localhost:8080`). Runs on `:8090` (8080/8081/8180 are taken by podcast-be/Expo/Keycloak respectively). No database of its own — this service is stateless.
- `mvn test` — the surefire plugin passes `-Dnet.bytebuddy.experimental=true`; without it, `@MockBean`-based tests fail to self-attach Mockito's inline mock maker on JDKs newer than Byte Buddy officially supports (JDK 25 on this machine vs. the project's Java 21 target). Safe/inert on JDKs Byte Buddy does support — don't remove it just because your local JDK doesn't need it.

## Generated OpenAPI client

`api/podcast-openapi.yaml` is a **vendored copy** of `skateboard-podcast-be/api/openapi.yaml` — this repo doesn't own that contract, and there's no shared contract registry between the two repos. When the upstream spec changes, re-copy it here by hand, and check whether `PodcastController`'s `@PreAuthorize` authorities still match its `x-required-permissions`.

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
- **Authorization boundary**: `@PreAuthorize` authorities on `PodcastController` are a deliberate, coarse-grained duplication of `x-required-permissions` from the vendored spec — an API-boundary check on token claims, not domain logic, so it doesn't conflict with keeping business rules downstream. Fine-grained/domain authorization still lives in `skateboard-podcast-be`.
- **Error shape** (`exception/GlobalExceptionHandler`, `exception/DownstreamServiceException`, `exception/ErrorResponse`): every error the frontend sees — from a controller, from `@PreAuthorize`, or from a rejected-before-dispatch auth failure (`web/RestAuthenticationEntryPoint`) — comes back as the same `{code, message, correlationId, timestamp}` shape. `PodcastClient` maps downstream 5xx/connectivity failures to `PODCAST_SERVICE_UNAVAILABLE` (503); downstream 4xx is passed through with its original status (that's the downstream service's own validation/not-found result, not an outage).

## Tests

JUnit 5 (+ AssertJ, spring-security-test). Two classes:
- `exception/GlobalExceptionHandlerTest` — plain unit test of the exception → `ErrorResponse` mapping (no Spring context).
- `controller/PodcastControllerSecurityTest` — `@WebMvcTest` + `@Import(SecurityConfig.class)`, verifies the BFF's own auth gate (no token → 401, wrong authority → 403, correct authority → 200) with `PodcastService` mocked out. This does **not** test `skateboard-podcast-be`'s behavior — that's `PodcastClient`'s and, downstream, `skateboard-podcast-be`'s own test suite's job.
