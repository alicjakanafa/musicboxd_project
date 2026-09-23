## Security Configuration Tests

`SecurityConfigTest`
(`src/test/java/com/example/MusicBoxd/Config/SecurityConfigTest.java`) and
`SecurityConfigLogoutHandlerTest`
(`src/test/java/com/example/MusicBoxd/Config/SecurityConfigLogoutHandlerTest.java`) verify
the authorization rules, login redirect, and logout redirect configured by `SecurityConfig`
(`src/main/java/com/example/MusicBoxd/Config/SecurityConfig.java`).

### Test setup

`SecurityConfigTest` is a `@WebMvcTest` slice. It needs at least one controller to stand up
a minimal MVC context, so it uses `AuthController` purely as a vehicle - these tests are
about `SecurityConfig`'s behaviour, not `AuthController`'s (see
[`testAuthController.md`](testAuthController.md) for that). See
[`testSupportHelpers.md`](testSupportHelpers.md) for why `TestOAuth2Config` is imported,
why `okta.oauth2.issuer` is overridden to an `.invalid` URL, and why
`OAuth2ClientAutoConfiguration`/`OAuth2ClientWebSecurityAutoConfiguration` are excluded.

`SecurityConfigLogoutHandlerTest` instead calls the private `LogoutHandler` built by
`SecurityConfig#logoutHandler()` directly (via `ReflectionTestUtils`), bypassing the full
`HttpSecurity`/`SecurityFilterChain` machinery. This is the only practical way to exercise
the handler's `catch (IOException e)` branch, since `HttpServletResponse.sendRedirect(...)`
essentially never throws in a normal `MockMvc` request. Because
`ServletUriComponentsBuilder.fromCurrentContextPath()` (used to build the `returnTo` URL)
reads the current request from `RequestContextHolder` rather than from the request object
passed into `LogoutHandler.logout(...)`, the test binds one with
`RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))` before
invoking the handler, mirroring what a real servlet container does automatically.

### What each test verifies

`SecurityConfigTest`:

- **`permitsUnauthenticatedAccessToStaticResourcePaths`** — an unauthenticated `GET
  /css/site.css` is not redirected to the OAuth2 login flow; the request reaches (empty)
  MVC handling and results in `404`, not a `3xx` redirect.
- **`permitsUnauthenticatedAccessToSpotifyLoginAndCallbackPaths`** — the same holds for
  `GET /spotify/login` and `GET /spotify/callback`.
- **`redirectsUnauthenticatedRequestsForProtectedPathsToOAuth2Login`** — an unauthenticated
  `GET /register` (a protected path not in the `permitAll` list) is redirected (`3xx`) to
  `/oauth2/authorization/okta`.
- **`allowsAuthenticatedRequestsForProtectedPaths`** — the same path returns `200` once
  authenticated via `OidcTestUsers.oidcUser(...)`.
- **`logoutRedirectsToTheOktaLogoutEndpointWithClientIdAndReturnTo`** — an authenticated
  `GET /logout` redirects to
  `{issuer}v2/logout?client_id={clientId}&returnTo={baseUrl}`, built from the
  `okta.oauth2.issuer`/`okta.oauth2.client-id` properties and the current request's base
  URL.

`SecurityConfigLogoutHandlerTest`:

- **`redirectsToTheConfiguredIssuersLogoutEndpoint`** — the happy path: given an `issuer`
  and `clientId`, the handler redirects to the expected Okta logout URL.
- **`wrapsAnIOExceptionFromSendRedirectInARuntimeException`** — if
  `HttpServletResponse.sendRedirect(...)` throws `IOException`, the handler wraps it in a
  `RuntimeException` (matching `SecurityConfig#logoutHandler()`'s `catch` block) instead of
  swallowing it silently.

### Known limitation / suspected production defect

`SecurityConfig` configures `oauth2Login(...)` without a custom `.loginPage(...)`. Spring
Security therefore installs its own `DefaultLoginPageGeneratingFilter`, which
unconditionally intercepts every `GET /login` request - authenticated or not - before it
can reach `AuthController#login()`. In practice this means the application's own
`login.html` view is never actually served; every request to `/login` gets Spring
Security's generic "Please sign in" page instead. `redirectsUnauthenticatedRequestsForProtectedPathsToOAuth2Login`
and `allowsAuthenticatedRequestsForProtectedPaths` therefore use `/register` (not `/login`)
as the example protected path, to test `SecurityConfig`'s general authorization behaviour
without being confused by that interaction. See
[`testAuthController.md`](testAuthController.md) for the test that documents the `/login`
behaviour directly, and revisit both if `SecurityConfig` is ever updated to call
`oauth2Login().loginPage("/login")` explicitly.

The `onAuthenticationSuccess` redirect to `/users/after-login` (the success handler
registered via `oauth2Login(oauth2 -> oauth2.successHandler(...))`) is not covered by any
test: reaching it requires a real OAuth2 authorization-code exchange, which a `@WebMvcTest`
slice using `SecurityMockMvcRequestPostProcessors.oidcLogin()` deliberately bypasses (that
processor injects an already-authenticated principal directly into the `SecurityContext`
rather than driving the actual login handshake). Covering it would require a heavier
`@SpringBootTest` with a stubbed authorization server (for example via WireMock), which was
judged out of proportion for this application's needs.

### Running the tests

```
./mvnw -Dtest=SecurityConfigTest,SecurityConfigLogoutHandlerTest test
```

or, as part of the full suite:

```
./mvnw test
```
