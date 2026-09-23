## About Controller Tests

`AboutControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/AboutControllerTest.java`) is a
`@WebMvcTest` slice covering `AboutController`
(`src/main/java/com/example/MusicBoxd/Controller/AboutController.java`), a static page
controller with no collaborators of its own.

### Test setup

Standard `SecurityConfig` + `TestOAuth2Config` import; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md).

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — `GET /about` without authentication redirects
  to the OIDC login flow (every route requires authentication under `SecurityConfig`).
- **`returnsAboutViewForAuthenticatedUser`** — an authenticated request returns `200` with
  view name `about`.

### Running the tests

```
./mvnw -Dtest=AboutControllerTest test
```
