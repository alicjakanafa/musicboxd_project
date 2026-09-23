## Last.fm Controller Tests

`LastFmControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/LastFmControllerTest.java`) is a
`@WebMvcTest` slice covering `lastFmController`
(`src/main/java/com/example/MusicBoxd/Controller/lastFmController.java`), the controller
behind the `/top-40` chart page. `LastFmService` and `ArtistRepository` are `@MockitoBean`s.

### Test setup

Standard `SecurityConfig` + `TestOAuth2Config` import; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md).

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — `GET /top-40` without authentication redirects
  to the OIDC login flow.
- **`showsTopArtistsForAuthenticatedUser`** — an authenticated request renders `top-40` with
  the artist list from `LastFmService.getTopArtists()`.

### Running the tests

```
./mvnw -Dtest=LastFmControllerTest test
```
