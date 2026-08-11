# Skateboard UI Backend

## Overview

`skateboard-ui-backend` is the Backend for Frontend (BFF) and API entry point for the Skateboard frontend.

Its main responsibility is to sit between the frontend and the internal Skateboard microservices.

```text
skateboard-fe
     |
     | Keycloak JWT
     v
skateboard-ui-backend
     |
     +--> skateboard-podcast-be
     +--> skateboard-events-be
     +--> skateboard-spots-be
     +--> future services
```

The UI Backend should remain thin. It is responsible for authentication, authorization, API orchestration, downstream communication, error handling, and UI-specific responses.

Business/domain rules must remain inside the corresponding domain microservices.

---

## Main Responsibilities

The `skateboard-ui-backend` should handle:

- Keycloak JWT validation
- User authentication
- Role extraction and authorization
- Routing frontend requests to internal services
- API aggregation/orchestration
- UI-specific DTOs
- Downstream client configuration
- Token propagation
- Error translation
- Correlation IDs and logging
- Timeouts and resilience configuration

The UI Backend should **not** contain core domain logic.

For example:

```text
Can the user publish a podcast?
```

This rule belongs in:

```text
skateboard-podcast-be
```

However, combining data from multiple services for a frontend screen belongs in the BFF.

Example:

```text
GET /api/home
```

The UI Backend may internally call:

```text
Podcast Service
Event Service
Spot Service
```

and return one response designed for the frontend.

---

## Recommended Architecture

Use a simple layered BFF architecture:

```text
Controller
    |
    v
Service / Orchestration
    |
    v
Client Wrapper
    |
    v
Generated OpenAPI Client
    |
    v
Domain Microservice
```

Do not introduce a full Hexagonal Architecture unless the BFF becomes significantly more complex.

Domain services such as `skateboard-podcast-be` can continue using Hexagonal Architecture.

---

## Recommended Package Structure

```text
src/main/java/com/skateboard/uibackend
|
+-- config
|   +-- SecurityConfig.java
|   +-- WebClientConfig.java
|   +-- OpenApiClientConfig.java
|   +-- CorsConfig.java
|
+-- security
|   +-- KeycloakRoleConverter.java
|   +-- CurrentUser.java
|   +-- UserContext.java
|
+-- controller
|   +-- PodcastController.java
|   +-- UserController.java
|   +-- HomeController.java
|
+-- service
|   +-- PodcastService.java
|   +-- UserService.java
|   +-- HomeService.java
|
+-- client
|   +-- podcast
|   |   +-- PodcastClient.java
|   |
|   +-- events
|   |   +-- EventClient.java
|   |
|   +-- spots
|       +-- SpotClient.java
|
+-- dto
|   +-- request
|   +-- response
|
+-- exception
|   +-- GlobalExceptionHandler.java
|   +-- DownstreamServiceException.java
|
+-- SkateboardUiBackendApplication.java
```

Generated OpenAPI code should not be mixed directly with handwritten application code.

Prefer generating it into:

```text
target/generated-sources/openapi
```

For a larger project, generated clients can later be moved into separate Maven modules.

---

## Security Flow

The frontend authenticates directly with Keycloak.

Recommended flow:

```text
User
 |
 v
skateboard-fe
 |
 | Authorization Code + PKCE
 v
Keycloak
 |
 | Access Token
 v
skateboard-fe
 |
 | Authorization: Bearer <JWT>
 v
skateboard-ui-backend
 |
 | Validate JWT
 | Extract roles
 | Apply authorization
 v
Internal Microservices
```

Configure `skateboard-ui-backend` as a Spring Security OAuth2 Resource Server.

The UI Backend should validate:

- JWT signature
- issuer
- expiration
- roles/authorities

Never trust roles sent manually by the frontend.

Avoid:

```http
X-User-Role: ADMIN
```

The BFF must derive roles from the validated Keycloak token.

---

## Token Propagation

For the current architecture, the recommended starting point is to propagate the user JWT to internal services that also need the authenticated user context.

```text
Frontend
   |
   | User JWT
   v
UI Backend
   |
   | Same User JWT
   v
Podcast Service
```

This allows the downstream service to independently validate the token and apply authorization.

If service-to-service authentication is introduced later, the model can evolve to:

```text
Frontend
   |
   | Keycloak User JWT
   v
UI Backend
   |
   | Service/Application Token
   | + Trusted User Context
   v
Internal Service
```

Do not introduce this additional complexity until required.

---

## OpenAPI Client Strategy

For known internal microservices, use **typed OpenAPI-generated clients**.

Example:

```text
skateboard-podcast-be
        |
        | OpenAPI Contract
        v
podcast-api.yaml
        |
        | OpenAPI Generator
        v
PodcastApi
PodcastDto
CreatePodcastRequest
UpdatePodcastRequest
```

The UI Backend should consume the generated client through a handwritten wrapper.

Recommended flow:

```text
PodcastController
       |
       v
PodcastService
       |
       v
PodcastClient
       |
       v
Generated PodcastApi
       |
       v
skateboard-podcast-be
```

Avoid calling generated APIs directly from controllers.

Bad:

```java
@RestController
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastApi podcastApi;
}
```

Preferred:

```java
@RestController
@RequiredArgsConstructor
public class PodcastController {

    private final PodcastService podcastService;
}
```

Service:

```java
@Service
@RequiredArgsConstructor
public class PodcastService {

    private final PodcastClient podcastClient;

    public List<PodcastDto> getPodcasts() {
        return podcastClient.getPodcasts();
    }
}
```

Client wrapper:

```java
@Component
@RequiredArgsConstructor
public class PodcastClient {

    private final PodcastApi podcastApi;

    public List<PodcastDto> getPodcasts() {
        return podcastApi.getPodcasts();
    }
}
```

The wrapper provides an abstraction between the BFF and generated code.

It also makes future changes to:

- authentication
- generated client implementation
- OpenAPI versions
- exception handling
- retries
- request headers

easier to manage.

---

## Generic Client vs Generated Client

Use generated clients for known internal services.

```text
Known internal service
        |
        v
Generated OpenAPI Client
```

Use a generic HTTP client only when the destination or API is genuinely dynamic.

Avoid building the BFF around a generic API such as:

```java
genericHttpClient.call(
    "podcast",
    "/api/v1/podcasts/" + id,
    HttpMethod.GET,
    null
);
```

Prefer:

```java
podcastApi.getPodcastById(id);
```

This provides:

- compile-time validation
- typed DTOs
- better IDE support
- easier refactoring
- clearer API contracts
- less manual HTTP handling

---

## Generic Infrastructure

Although downstream APIs should remain typed, common HTTP infrastructure should be shared.

Common configuration can include:

```text
timeouts
authentication
token propagation
logging
correlation IDs
error handling
retry policy
metrics
WebClient configuration
```

Example configuration:

```yaml
clients:
  podcast:
    base-url: http://skateboard-podcast-be:8080

  events:
    base-url: http://skateboard-events-be:8080

  spots:
    base-url: http://skateboard-spots-be:8080
```

Each service should still have its own typed client wrapper.

```text
Shared HTTP Infrastructure
          |
   +------+------+
   |      |      |
   v      v      v
Podcast Event   Spot
Client  Client  Client
   |      |      |
   v      v      v
Generated OpenAPI Clients
```

---

## API Exposure

The frontend should only communicate with the UI Backend.

Recommended:

```text
skateboard-fe
      |
      v
skateboard-ui-backend
      |
      +--> skateboard-podcast-be
      +--> skateboard-events-be
      +--> skateboard-spots-be
```

Avoid:

```text
skateboard-fe
   |
   +--> skateboard-podcast-be
   +--> skateboard-events-be
   +--> skateboard-spots-be
```

The UI Backend should expose a stable frontend API such as:

```text
/api/podcasts/**
/api/events/**
/api/spots/**
/api/users/**
/api/home
```

Internal service topology should remain hidden from the frontend.

---

## API Aggregation

The BFF can expose UI-oriented endpoints that aggregate multiple services.

Example:

```text
GET /api/home
```

Implementation:

```text
HomeController
      |
      v
HomeService
      |
      +--> PodcastClient.getLatest()
      |
      +--> EventClient.getUpcoming()
      |
      +--> SpotClient.getPopular()
```

Example frontend response:

```json
{
  "podcasts": [],
  "upcomingEvents": [],
  "popularSpots": []
}
```

This prevents the frontend from having to understand or orchestrate the internal microservice architecture.

---

## Error Handling

The UI Backend should translate downstream errors into a consistent frontend error model.

Example:

```json
{
  "code": "PODCAST_SERVICE_UNAVAILABLE",
  "message": "Podcast service is currently unavailable",
  "correlationId": "..."
}
```

Recommended exception structure:

```text
exception
|
+-- GlobalExceptionHandler
+-- DownstreamServiceException
+-- UnauthorizedException
+-- ForbiddenException
```

Do not expose internal service stack traces or implementation details to the frontend.

---

## Logging and Correlation IDs

Every incoming request should have a correlation ID.

Recommended flow:

```text
Frontend
   |
   | X-Correlation-Id
   v
UI Backend
   |
   | X-Correlation-Id
   v
Internal Service
```

If the frontend does not provide one, the UI Backend can generate it.

The same value should be propagated to all downstream calls.

---

## Design Rules

### UI Backend

Responsible for:

- authentication boundary
- authorization at API boundary
- routing
- API aggregation
- orchestration
- user context
- frontend-specific DTOs
- error mapping
- downstream integration

### Domain Services

Responsible for:

- business rules
- domain model
- persistence
- domain authorization
- domain validation
- use cases
- service-specific data

Example:

```text
skateboard-podcast-be
```

owns podcast business rules.

```text
skateboard-events-be
```

owns event business rules.

```text
skateboard-spots-be
```

owns skate spot business rules.

---

## Avoid

Do not:

- expose internal microservices directly to the frontend
- trust roles or user IDs provided manually by the frontend
- put domain business rules inside the BFF
- build one generic URL-based proxy for all services
- call generated OpenAPI clients directly from controllers
- duplicate domain validation in the BFF
- tightly couple frontend routes to internal service URLs
- expose downstream implementation errors directly to the frontend

---

## Recommended Technology Stack

```text
Java 17+
Spring Boot
Spring Web / WebClient
Spring Security
OAuth2 Resource Server
Keycloak
OpenAPI Generator
Lombok
Maven
Docker
```

Use `WebClient` for downstream integrations.

Generated OpenAPI clients may also use WebClient depending on the OpenAPI Generator configuration.

---

## Final Architecture

```text
                         Keycloak
                            ^
                            |
                       Authentication
                            |
                            |
skateboard-fe --------------+
      |
      | Keycloak JWT
      v
+--------------------------------+
| skateboard-ui-backend          |
|                                |
| controller                     |
|      |                         |
|      v                         |
| service / orchestration        |
|      |                         |
|      v                         |
| client wrappers                |
|      |                         |
|      v                         |
| generated OpenAPI clients      |
|                                |
| security                       |
| configuration                  |
| exception handling             |
+---------------+----------------+
                |
        +-------+--------+
        |       |        |
        v       v        v
   podcast-be events-be spots-be
        |       |        |
      domain   domain   domain
        |       |        |
       DB      DB       DB
```

---

## Implementation Recommendation

Start the project with:

1. Create `skateboard-ui-backend`.
2. Configure Spring Security as an OAuth2 Resource Server.
3. Configure Keycloak JWT validation.
4. Implement role extraction.
5. Add the first frontend endpoint.
6. Generate the `skateboard-podcast-be` client from its OpenAPI specification.
7. Wrap the generated client inside `PodcastClient`.
8. Add `PodcastService`.
9. Expose the functionality through `PodcastController`.
10. Configure token propagation.
11. Add consistent downstream error handling.
12. Add correlation ID propagation.
13. Add additional service clients only when needed.
14. Introduce aggregation endpoints when the frontend needs data from multiple services.

Keep the BFF small and focused.

The preferred architecture is:

```text
Thin BFF
+
Typed OpenAPI Clients
+
Shared HTTP Infrastructure
+
Domain-Oriented Microservices
```
