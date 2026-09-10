---
name: add-bff-endpoint
description: >-
  Add or change a frontend-facing route on this ui-backend BFF that proxies a
  downstream Skateboard service (podcast / user / app-config / notification), or
  wire in a brand-new downstream service. Use whenever the task is "expose
  endpoint X to the frontend", "proxy the new <service> route", "the upstream
  spec changed", or "add a client for <new service>". Covers the vendored spec,
  the pom generator execution, the ApiConfig bean, the Client/Service/Controller
  trio, the @PreAuthorize / SecurityConfig auth gate, bff-openapi.yaml, and the
  two test shapes.
---

# Adding / changing a BFF endpoint

This BFF is a **thin pass-through**. Every route follows the same vertical slice:

```
Controller (@PreAuthorize mirrors x-required-permissions)
  -> Service   (thin today; the seam for future aggregation — keep it, even if it only delegates)
  -> Client    (blocks on the generated Mono/Flux, maps failures to DownstreamServiceException)
  -> <Service>Api (generated from a vendored spec) --WebClient--> downstream service
```

Read `CLAUDE.md` first — it is the source of truth for *why* each layer exists. The
one aggregating feature (`HomeController`/`HomeService`) is the exception, not the model.

## Before writing code

1. **Find the upstream contract.** The downstream service owns the contract; this
   repo vendors a copy. Locate the operation in the sibling repo's
   `api/openapi.yaml` (`../skateboard-podcast-be`, `../skateboard-user-be`,
   `../skateboard-app-config-be`, `../skateboard-notification-be`).
2. **Read its `x-required-permissions`.** That list — verbatim, no `ROLE_`/`SCOPE_`
   prefix — is what the controller's `@PreAuthorize` must check. No entry on the
   operation => the route is pre-auth (see step "Auth gate").
3. **Decide the layer of change** using the decision table below.

## Decision table

| Situation | Do |
| --- | --- |
| New route on an **existing** downstream service whose operation is **already in the vendored spec** | Add Client method -> Service method -> Controller method -> bff-openapi entry -> tests. No regen needed unless the generated `*Api` lacks the method. |
| Operation **missing / changed** in the vendored spec | Re-vendor the spec first (next section), rebuild to regenerate, then proceed. |
| **New downstream service** (no `client/<name>/` yet) | Full "New downstream service" checklist below. Events/spots do **not** exist — only add a service that is real and callable. |
| Aggregating two+ services | This is `HomeService`-shaped. Put the merge in the Service layer, add a resolver/strategy rather than branching, and make every downstream failure degrade (fall back / return `null` -> `204`), never 500. |

## Re-vendoring a changed upstream spec

```bash
cp ../skateboard-<name>-be/api/openapi.yaml api/<name>-openapi.yaml
```

Then:

- Re-add the leading `# Vendored copy of …` header comment block (the drift script
  ignores it; humans need it).
- `bash scripts/check-vendored-specs.sh --warn-only` — confirm the only drift is
  what you intended. podcast/user/notification carry known pre-existing hand
  edits; app-config must stay clean.
- Diff each affected controller's `@PreAuthorize` against the new
  `x-required-permissions`.
- `mvn package` regenerates `client/<name>/generated/**` — never hand-edit those.

## New downstream service checklist

1. **Vendor the spec**: `api/<name>-openapi.yaml` with the `# Vendored copy` header.
2. **pom.xml**: add a `<execution>` to `openapi-generator-maven-plugin` — copy an
   existing block (`app-config-client`), change the `<id>`, `<inputSpec>`, and the
   three `*Package` lines to `com.skateboard.uibackend.client.<name>.generated.{api,model,invoker}`.
   Keep all `<configOptions>` identical.
3. **scripts/check-vendored-specs.sh**: add `"api/<name>-openapi.yaml:../skateboard-<name>-be"` to `mappings`.
4. **ClientsProperties**: add a nested `<Name>` static class (baseUrl +
   connect/read timeouts, defaults `3000`/`5000`), a field, and a
   `@NestedConfigurationProperty` getter.
5. **application.yml AND application-railway.yml**: add `clients.<name>.*` with an
   env-var-overridable `base-url` (pick a free port — 8080 podcast, 8082 user,
   8083 app-config, 8084 notification, 8090 this app, 8180 Keycloak).
6. **config/<Name>ApiConfig.java**: copy `AppConfigApiConfig` — one `ApiClient`
   `@Bean` wiring `webClientBuilder` + `bearerTokenExchangeFilter` +
   `correlationIdExchangeFilter` + `apiClient.setBasePath(config.getBaseUrl())`,
   then one `@Bean` per generated `*Api`.
7. **client/<name>/<Name>Client.java**: copy `PodcastClient`. Blocks on every call
   via `call(...)` / `callList(...)`. `mapResponseException`: 5xx + connectivity ->
   `<NAME>_SERVICE_UNAVAILABLE` (503); 4xx passed through with original status and
   a `<NAME>_*` code. Only add the CampaignClient-style "surface the downstream
   `message`" logic when upstream error messages are specific and user-actionable.
8. **service/<Name>Service.java**: `@Service`, constructor-injects the client,
   one delegating method per operation. Keep it even though it only delegates.
9. **controller/<Name>Controller.java**: `@RestController`, routes under
   `/api/<name>/**`. Each method: `@PreAuthorize("hasAuthority('FUNC_…')")`
   mirroring that operation's `x-required-permissions` (per-route, not
   per-controller — check each one). `@ResponseStatus(HttpStatus.CREATED/NO_CONTENT)`
   where the upstream returns 201/204. A `null` service return that means
   "downstream 204 / nothing" -> return `204` (`ResponseEntity.noContent()`), not an error.
10. **api/bff-openapi.yaml**: hand-add the path(s). This is the frontend's
    contract — declare the real response shape, `x-required-permissions`, and a
    `503` for any route that calls downstream. Keep the `ErrorResponse` schema
    (`{code, message, correlationId, timestamp}`).
11. **SecurityConfig**: only if the route is pre-auth — add a `requestMatchers(METHOD, "/api/<name>/…").permitAll()` line with a comment saying why, above `anyRequest().authenticated()`.
12. **Tests** (see below).
13. `mvn package` and run `bash scripts/check-vendored-specs.sh --warn-only`.

## Auth gate rules

- Authority strings come **verbatim** from the JWT `authorities` claim — use the
  exact `FUNC_*` string from `x-required-permissions`, no prefix.
- Per-route: one controller can mix authorities (`AboutUsController`: viewer route
  is `FUNC_TAB_SETTINGS`, the three admin routes are `FUNC_ABOUT_US_MANAGE`).
- Pre-auth (no `x-required-permissions`): no `@PreAuthorize`, plus an explicit
  `permitAll()` in `SecurityConfig` (e.g. `/api/config`, `/api/campaigns/active`,
  `/api/campaigns/*/events`). The token relay still forwards a bearer if present.
- No `AudienceValidator` here — downstream validates `aud`.

## Tests (both shapes required)

**`controller/<Name>ControllerSecurityTest`** — `@WebMvcTest(controllers = …)` +
`@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class})`, service
`@MockBean`. This tests *this BFF's own gate only*. Per protected route assert:

- no token -> `401`, `jsonPath("$.code").value("UNAUTHENTICATED")`
- wrong authority -> `403`, `$.code == "FORBIDDEN"`
- correct authority -> `2xx` (stub the service)
- pre-auth routes -> `2xx`/`204` with no token

**`client/<name>/<Name>ClientTest`** — plain JUnit, `@Mock` the generated `*Api`,
`MockitoAnnotations.openMocks`, construct the client by hand. Assert:

- success passes through (`isSameAs`)
- downstream `404`/`400`/`409` -> `DownstreamServiceException` with the right
  status + `<NAME>_*` code (+ downstream message if that client surfaces it)
- `5xx` and `WebClientRequestException` -> `503` / `<NAME>_SERVICE_UNAVAILABLE`

Do **not** write downstream-behaviour tests here — that is the downstream
service's own suite.

## Build notes

- `mvn package` runs the generator, compiles, tests. `mvn test` needs
  `-Dnet.bytebuddy.experimental=true` (surefire already passes it).
- Two deps (`javax.annotation-api`, `jsr305`) exist only so generated code
  compiles — never reference `javax.annotation.*` in hand-written code, use
  `jakarta.*`.
- Multipart upload: the java/webclient generator maps a binary field to
  `java.io.File`, so the client must materialize the `MultipartFile` to a temp
  file and delete it in a `finally` (see `CampaignClient.uploadCampaignScreenImage`).
