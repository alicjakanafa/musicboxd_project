## Last.fm Service Tests

`LastFmServiceTest`
(`src/test/java/com/example/MusicBoxd/api/lastfm/LastFmServiceTest.java`) is a plain
JUnit unit test (no Spring context) that verifies the
[`LastFmService`](../../features/ExternalMusicApis/lastFmServiceDocumentation.md)
builds the correct outgoing request and parses a realistic Last.fm `chart.gettopartists`
response correctly.

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

### Known limitation covered by this test, not by production behavior

The sample response bodies used in these tests use the JSON key `text` for each image's
URL, to match the `LastFmImage.text` field exactly. The real Last.fm API actually
returns `#text` for that key. See the "Known limitation" section in the
[`LastFmService` documentation](../../features/ExternalMusicApis/lastFmServiceDocumentation.md#known-limitation)
— these tests intentionally use the field name Java expects so they exercise
deserialization correctly, but that means they do not reproduce the real API's `#text`
key mismatch. That mismatch was left as-is because fixing it is a production code
change outside the scope of this test suite.

### Running the tests

```
./mvnw -Dtest=LastFmServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
