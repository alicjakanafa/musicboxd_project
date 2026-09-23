## Last.fm Service Tests

`LastFmServiceTest`
(`src/test/java/com/example/MusicBoxd/api/lastfm/LastFmServiceTest.java`) is a plain
JUnit unit test (no Spring context) that verifies every public method of
[`LastFmService`](../../features/ExternalMusicApis/lastFmServiceDocumentation.md)
(`getTopArtists`, `getArtistInfo`, `getArtistAlbums`, `searchAlbums`, `getAlbumInfo`)
builds the correct outgoing request and parses realistic Last.fm API responses
correctly, including the error-handling paths of `searchAlbums` and `getAlbumInfo`.

### Test setup

`LastFmService` builds its own private `RestTemplate` internally rather than accepting
one via constructor injection, so the test does not use `@SpringBootTest` or any mock
bean wiring. Instead, each test:

1. Instantiates `LastFmService` directly with `new LastFmService("test-api-key")`
   (bypassing the `@Value("${lastfm.api.key}")` injection Spring would normally do).
2. Pulls the service's private `restTemplate` field out with
   `ReflectionTestUtils.getField(...)`.
3. Binds a `MockRestServiceServer` to that extracted `RestTemplate` with
   `MockRestServiceServer.bindTo(restTemplate).build()`.
4. Stubs the expected outgoing request and its response with
   `.expect(requestTo(...))` / `.andRespond(withSuccess(...))`.

No real network call to `ws.audioscrobbler.com` is ever made.

### What each test verifies

- **`getTopArtistsBuildsExpectedRequestAndParsesResponse`** — `getTopArtists()` issues a
  GET to `https://ws.audioscrobbler.com/2.0/` with `method=chart.gettopartists`,
  `api_key=test-api-key`, `format=json`, `limit=40`, and correctly deserializes a
  two-artist sample response into a `LastFmResponse`: every field of each
  `LastFmArtist` (`name`, `playcount`, `listeners`, `mbid`, `url`) is checked, along
  with the nested `LastFmImage` list (including an artist with an empty `image` list).
- **`getTopArtistsUsesInjectedApiKeyInRequest`** — a second `LastFmService` instance
  constructed with a different API key sends that key as the `api_key` query parameter,
  confirming the constructor-injected key (not a hardcoded value) drives the request.
- **`getArtistInfoBuildsExpectedRequestAndParsesResponse`** — `getArtistInfo("Radiohead")`
  issues GETs with `method=artist.getinfo`, `artist=Radiohead`, `autocorrect=1`, and
  correctly deserializes `name`, `listeners`, `playcount`, and the nested `bio`
  (`summary`/`content`). The mock server is set up with `ExpectedCount.times(2)` because
  `getArtistInfo` fetches the same URL twice (see the service documentation).
- **`getArtistAlbumsBuildsExpectedRequestAndParsesResponse`** —
  `getArtistAlbums("Radiohead")` issues a GET with `method=artist.gettopalbums`,
  `limit=50`, `autocorrect=1`, and correctly deserializes the nested `topalbums.album`
  list, including each album's `image` list.
- **`searchAlbumsBuildsExpectedRequestAndParsesResponse`** — `searchAlbums("OK Computer")`
  issues a GET with `method=album.search`, `album=OK+Computer`, `limit=20`, and
  correctly deserializes the nested `results.albummatches.album` list.
- **`searchAlbumsReturnsNullWhenRemoteServiceReturnsServerError`** — when the mock server
  responds with an HTTP 500, `searchAlbums` catches the resulting exception and returns
  `null` instead of propagating it.
- **`getAlbumInfoBuildsExpectedRequestAndParsesResponse`** —
  `getAlbumInfo("Radiohead", "OK Computer")` issues a GET with `method=album.getinfo`,
  `artist=Radiohead`, `album=OK+Computer`, `autocorrect=1`, and correctly deserializes
  the album's `name`, `artist`, `releasedate`, and its `tracks.track` list (including
  `LastFmTrack.getFormattedDuration()` converting a `284`-second duration to `4:44`).
- **`getAlbumInfoReturnsNullWhenRemoteServiceReturnsNotFound`** — when the mock server
  responds with an HTTP 404 (an unknown artist/album pair), `getAlbumInfo` catches the
  exception and returns `null` instead of propagating it.

### Running the tests

```
./mvnw -Dtest=LastFmServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
