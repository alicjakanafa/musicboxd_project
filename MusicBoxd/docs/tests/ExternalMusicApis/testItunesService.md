## iTunes Service Tests

`ItunesServiceTest`
(`src/test/java/com/example/MusicBoxd/api/itunes/ItunesServiceTest.java`) is a plain
JUnit unit test (no Spring context) that verifies the
[`ItunesService`](../../features/ExternalMusicApis/itunesServiceDocumentation.md)
builds the correct outgoing request and parses realistic iTunes Search API responses
correctly.

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

### Running the tests

```
./mvnw -Dtest=ItunesServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
