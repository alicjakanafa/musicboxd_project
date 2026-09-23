## Auth Controller Tests

`AuthControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/AuthControllerTest.java`) is a
`@WebMvcTest` slice covering `AuthController`
(`src/main/java/com/example/MusicBoxd/Controller/AuthController.java`) together with
`SecurityConfig`'s authorization rules and `WebConfig`'s uploads resource handler, since
`AuthController` has no collaborators of its own and makes a convenient, lightweight
vehicle for exercising both.

### Test setup

See [`testSupportHelpers.md`](testSupportHelpers.md) for the shared `TestOAuth2Config` /
`OidcTestUsers` helpers and the reasoning behind the `okta.oauth2.issuer` test property and
excluded auto-configurations used here. `WebConfig` is imported alongside `SecurityConfig`
so the `/uploads/**` tests exercise the real resource handler rather than a stub.

The uploads tests create and clean up a real file under the project's `uploads/` directory
(relative to the working directory the tests run from) in `@AfterEach`, since that is
exactly where `WebConfig` points its resource handler (`file:uploads/`) - see
[`testWebConfig.md`](testWebConfig.md) for the lower-level unit test of that wiring.

### What each test verifies

- **`loginPathIsInterceptedBySpringSecuritysGeneratedPageInsteadOfAuthControllersView`** —
  documents a suspected production defect: `GET /login`, even when authenticated via
  `OidcTestUsers.oidcUser(...)`, is answered by Spring Security's own generated "Please
  sign in" page, not by `AuthController#login()`'s `login` view. See
  [`testSecurityConfig.md`](testSecurityConfig.md#known-limitation--suspected-production-defect)
  for the full explanation.
- **`showsRegisterViewForAuthenticatedUser`** — an authenticated `GET /register` returns
  `200` with view name `register`.
- **`redirectsUnauthenticatedUserAwayFromRegister`** — an unauthenticated `GET /register`
  is redirected (`3xx`) rather than served.
- **`servesAnExistingFileFromTheUploadsDirectoryForAnAuthenticatedUser`** — an authenticated
  request for a file that actually exists under `uploads/` is served with `200` and the
  file's exact contents, proving `WebConfig`'s `/uploads/**` → `file:uploads/` mapping
  works end-to-end through `MockMvc`.
- **`returnsNotFoundForAMissingFileUnderTheUploadsDirectory`** — an authenticated request
  for a file that does not exist under `uploads/` returns `404`.

### Running the tests

```
./mvnw -Dtest=AuthControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
