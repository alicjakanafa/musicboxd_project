## iTunes Service Tests

`ItunesServiceTest`
(`src/test/java/com/example/MusicBoxd/api/itunes/ItunesServiceTest.java`) is a plain
JUnit unit test (no Spring context) that verifies every public method of
[`ItunesService`](../../features/ExternalMusicApis/itunesServiceDocumentation.md)
(`searchAlbums`, `searchAlbumsByArtist`, `searchTracks`, `getDailyAlbum`, and
`getAlbumTracks`) builds the correct outgoing request and parses realistic iTunes
Search/Lookup API responses correctly.

### Test setup

`ItunesService` builds its own private `RestTemplate` internally rather than accepting
one via constructor injection, so the test does not use `@SpringBootTest` or any mock
bean wiring. Instead, each test:

1. Instantiates `ItunesService` directly with `new ItunesService()`.
2. Pulls the service's private `restTemplate` field out with
   `ReflectionTestUtils.getField(...)`.
3. Binds a `MockRestServiceServer` to that extracted `RestTemplate` with
   `MockRestServiceServer.bindTo(restTemplate).build()`.
4. Stubs the expected outgoing request and its response with
   `.expect(requestTo(...))` / `.andRespond(withSuccess(...))`.

No real network call to `itunes.apple.com` is ever made.

### What each test verifies

- **`searchAlbumsBuildsExpectedRequestAndParsesResponse`** — `searchAlbums("1989")`
  issues a GET to `https://itunes.apple.com/search` with `term=1989`, `media=music`,
  `entity=album`, `limit=20`, and correctly deserializes a sample JSON response into an
  `ItunesAlbumResponse` whose `resultCount` and every field of the single
  `ItunesAlbum` result match the response body.
- **`searchAlbumsParsesResponseServedAsTextJavascriptContentType`** — when the mock
  server responds with `Content-Type: text/javascript` (the content type the real
  iTunes API actually uses) instead of `application/json`, `searchAlbums(...)` still
  deserializes the body correctly. This exercises the custom message-converter setup in
  `ItunesService`'s constructor, which is the whole reason that setup exists.
- **`searchTracksBuildsExpectedRequestAndParsesResponse`** — `searchTracks("karma
  police")` issues a GET with `entity=song` (instead of `album`) and a URL-encoded
  multi-word search term, and correctly deserializes a sample response into an
  `ItunesTrackResponse` with every `ItunesTrack` field populated.
- **`searchAlbumsReturnsEmptyResultsWhenNoMatchesFound`** — a response with
  `resultCount: 0` and an empty `results` array is passed straight through as an empty
  list, without the service throwing or substituting a default value.
- **`searchAlbumsByArtistBuildsExpectedRequestWithHighResultLimit`** —
  `searchAlbumsByArtist("Radiohead")` issues a GET with `entity=album` and `limit=200`
  (instead of the `20` used by `searchAlbums`), and deserializes the response the same
  way `searchAlbums` does.
- **`getAlbumTracksBuildsLookupRequestWithCollectionIdAndSongEntity`** —
  `getAlbumTracks(111L)` issues a GET to `https://itunes.apple.com/lookup` with
  `id=111` and `entity=song`, and deserializes the response into an
  `ItunesTrackResponse`.
- **`getDailyAlbumReturnsNullWhenSearchResultsAreEmpty`** — when the genre search
  returns `resultCount: 0` with an empty `results` array, `getDailyAlbum` returns `null`.
- **`getDailyAlbumReturnsNullWhenResultsFieldIsExplicitlyNull`** — the same null result
  when the `results` field is present but explicitly `null` rather than an empty array.
- **`getDailyAlbumReturnsNullWhenRemoteResponseBodyIsEmpty`** — an empty HTTP response
  body (no JSON at all) also results in `getDailyAlbum` returning `null`, rather than
  throwing.
- **`getDailyAlbumReturnsAnAlbumFromTheSearchResultsForAGivenUser`** — given a
  three-album search response, `getDailyAlbum(userId)` returns one of the three albums
  (not `null`, and not a fabricated value).
- **`getDailyAlbumReturnsSameAlbumForSameUserOnSameDay`** — calling `getDailyAlbum` twice
  in a row with the same `userId` (and therefore the same day) returns the exact same
  album both times, confirming the per-day/per-user seeded `Random` is deterministic
  rather than picking a fresh random album on every call.
- **`getDailyAlbumWorksWithoutAUserIdUsingEpochDaySeed`** — passing `null` for `userId`
  still returns a valid album, exercising the fallback seed based on
  `LocalDate.now().toEpochDay()`.

### Running the tests

```
./mvnw -Dtest=ItunesServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
