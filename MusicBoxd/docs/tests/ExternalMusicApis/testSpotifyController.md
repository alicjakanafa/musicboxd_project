## Spotify Controller Tests

`SpotifyController` is covered by two test classes:

- `SpotifyControllerTest`
  (`src/test/java/com/example/MusicBoxd/Controller/SpotifyControllerTest.java`) — a
  `@WebMvcTest(SpotifyController.class)` slice covering every HTTP endpoint
  (`/spotify/login`, `/spotify/callback`, `/spotify/player/current`,
  `/spotify/player/play|pause|next|previous`) with real security rules applied.
- `SpotifyControllerDataTest`
  (`src/test/java/com/example/MusicBoxd/Controller/SpotifyControllerDataTest.java`) — a
  plain JUnit test that instantiates `SpotifyController` directly to exercise
  `getSpotifyData`, `getTopArtists`, and `getTopTracks`, since none of the current MVC
  mappings call them; they are invoked instead by other controllers
  (`HomeController`, `ProfileController`) that inject this bean.

Both classes bind a `MockRestServiceServer` to a real `RestTemplate` bean
(`org.springframework.web.client.RestTemplate` from `RestTemplateConfig`) so no real
network calls are made to `accounts.spotify.com` or `api.spotify.com`. Response bodies
use realistic Spotify API JSON field names (`access_token`, `is_playing`,
`duration_ms`, `external_urls`, etc.), which also exercises the plain getter/setter DTOs
in `com.example.MusicBoxd.api.spotify` through normal Jackson deserialization.

### `SpotifyControllerTest` setup

`@WebMvcTest(SpotifyController.class)` with `@Import({SecurityConfig.class,
TestOAuth2Config.class, RestTemplateConfig.class})`, a `@MockitoBean UserRepository`
(required by `GlobalControllerAdvice`), and `@TestPropertySource` dummy values for the
Okta and `spotify.client.*` / `spotify.redirect.uri` properties. Authenticated requests
use `OidcTestUsers.oidcUser(...)`; the Spotify access token is placed directly into a
`MockHttpSession` under the `spotifyAccessToken` key the controller reads/writes.

### What `SpotifyControllerTest` verifies

- **`loginRedirectsToSpotifyAuthorizeUrlWithExpectedQueryParams`** /
  **`loginIsAccessibleWithoutAuthentication`** — `/spotify/login` redirects (without
  requiring authentication, per `SecurityConfig`'s `permitAll` list) to
  `https://accounts.spotify.com/authorize` with `client_id`, `response_type=code`,
  `redirect_uri`, and the full `scope` string.
- **`callbackExchangesCodeForTokenStoresItInSessionAndRedirectsHome`** —
  `/spotify/callback?code=...` POSTs a `grant_type=authorization_code` form body with
  HTTP Basic auth (base64 `clientId:clientSecret`) to
  `https://accounts.spotify.com/api/token`, stores the returned `access_token` in the
  session under `spotifyAccessToken`, and redirects to `/`.
- **`callbackThrowsWhenSpotifyReturnsNoTokenBody`** — a token endpoint response with no
  body causes the controller to throw `IllegalStateException` rather than silently
  storing a `null` token.
- **`current*` tests** — `/spotify/player/current` returns 401 when there is no
  `spotifyAccessToken` in session, requires authentication (unauthenticated request is
  redirected to the OIDC login), returns 204 when Spotify reports nothing playing
  (both an empty body and a body with no `item`), and returns 200 with the full
  currently-playing JSON (track name, artists, album, `is_playing`, `progress_ms`) when
  something is playing.
- **`play`/`pause`/`next`/`previous` tests** — each POST endpoint returns 401 with no
  session token, and otherwise forwards a bearer-authenticated request with the correct
  HTTP method (`PUT` for play/pause, `POST` for next/previous) to the matching
  `api.spotify.com/v1/me/player/...` endpoint.
- **`currentPropagatesSpotifyServerErrorAsUnhandledException`** /
  **`previousPropagatesSpotifyUnauthorizedResponseAsUnhandledException`** — document a
  currently unhandled path: `SpotifyController` does not catch `RestTemplate` exceptions,
  so an upstream Spotify 5xx/4xx response surfaces as an unhandled
  `HttpServerErrorException`/`HttpClientErrorException` (and therefore a generic 500 to
  the browser) rather than a controlled error response. See "Known limitations" below.

### `SpotifyControllerDataTest` setup

Plain JUnit (no Spring context): `new SpotifyController(new RestTemplate())`, with a
`MockRestServiceServer` bound to that `RestTemplate`. A `Model` is supplied as
`org.springframework.ui.ExtendedModelMap`.

### What `SpotifyControllerDataTest` verifies

- **`getSpotifyDataPopulatesModelWithListeningStatsFromRecentlyPlayedItems`** — given a
  4-item recently-played response (including a repeated track id, a `null` track, and a
  track with `null` id/artists/album), `getSpotifyData` computes
  `recentTracksCount` (all items), `uniqueTracksCount`/`uniqueArtistsCount`/
  `uniqueAlbumsCount` (distinct, filtering out nulls), and `listeningMinutes` (summed
  duration converted to minutes) correctly, sets `recentTrack` to the first item, and
  also populates `topTracks` and `currentlyPlaying`. Nested artist/album `images` in the
  response are asserted directly to exercise the `SpotifyImage` DTO.
- **`getSpotifyDataTwoArgOverloadDefaultsToMediumTermTimeRange`** — the two-argument
  overload forwards `medium_term` to the top-tracks request.
- **`getSpotifyDataFallsBackToMediumTermForAnInvalidTimeRange`** — an unrecognized
  `timeRange` value is normalized to `medium_term` before the top-tracks request is
  built (verified via the mock server's expected request URL).
- **`getSpotifyDataSetsZeroedStatsWhenRecentlyPlayedResponseBodyIsAbsent`** — a 204
  response from the recently-played endpoint results in all counts being `0`/empty
  rather than a `NullPointerException`.
- **`getSpotifyDataTreatsAnEmptyRecentlyPlayedItemsListAsNoRecentTrack`** — an empty
  (but non-null) `items` array results in a `null` `recentTrack` and zeroed counts.
- **`getTopArtistsBuildsRequestWithGivenTimeRangeAndParsesArtists`** /
  **`getTopArtistsFallsBackToMediumTermWhenTimeRangeIsNull`** — `getTopArtists` builds
  `.../v1/me/top/artists?time_range=<range>&limit=6` with the given range, or
  `medium_term` when `null` is passed, and parses the artist list.
- **`getTopTracksBuildsRequestWithGivenTimeRangeAndParsesTracks`** /
  **`getTopTracksFallsBackToMediumTermWhenTimeRangeIsInvalid`** — same behavior for
  `getTopTracks` against `.../v1/me/top/tracks`.

### Known limitations

- `SpotifyController` has no centralized handling for `RestTemplate` exceptions raised
  by failed Spotify API calls (see the two "unhandled exception" tests above); a real
  Spotify outage or an expired/invalid token on the player-control endpoints currently
  surfaces to the browser as a generic 500 rather than a meaningful error response.
- `SpotifyTokenResponse`'s `tokenType`, `expiresIn`, `refreshToken`, and `scope` getters,
  and `SpotifyCurrentlyPlayingResponse.setPlaying(boolean)`, are not exercised. The
  controller only ever reads `getAccessToken()` from the token response, and
  `@JsonProperty("is_playing")` is placed on the *field* (not the setter), so Jackson
  binds that value via field access rather than calling `setPlaying(...)`. Testing these
  directly would require getter/setter-only tests with no behavioral signal, so they are
  left uncovered.

### Running the tests

```
./mvnw -Dtest=SpotifyControllerTest,SpotifyControllerDataTest test
```

or, as part of the full suite:

```
./mvnw test
```
