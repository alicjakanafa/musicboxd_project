## Last.fm Service

`LastFmService` (`com.example.MusicBoxd.api.lastfm.LastFmService`) is a `@Service` that
wraps the [Last.fm API](https://www.last.fm/api) `chart.gettopartists` method so the
rest of the application can fetch a chart of currently popular artists without talking
to `RestTemplate` directly.

### What it provides

- `getTopArtists()` — fetches the current top-artists chart and returns a
  [`LastFmResponse`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmResponse.java),
  which nests a [`LastFmArtists`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmArtists.java)
  wrapper containing a `List<`[`LastFmArtist`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmArtist.java)`>`.
  Each artist has `name`, `playcount`, `listeners`, `mbid`, `url` and a
  `List<`[`LastFmImage`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmImage.java)`>`
  (each image has a `text` URL and a `size`, e.g. `small`/`medium`/`large`/`extralarge`).

### Request shape

`getTopArtists()` builds a GET request to `https://ws.audioscrobbler.com/2.0/` with the
following query parameters:

| Param       | Value                    |
|-------------|--------------------------|
| `method`    | `chart.gettopartists`    |
| `api_key`   | the configured API key   |
| `format`    | `json`                   |
| `limit`     | `40`                     |

### Configuration

`LastFmService`'s constructor takes the API key via `@Value("${lastfm.api.key}")`, so a
`lastfm.api.key` property (or equivalent environment variable /
`LASTFM_API_KEY`-style override) must be configured for the application context to
start this bean successfully. The key is sent as the `api_key` query parameter on every
request.

### Construction

Like `ItunesService`, `LastFmService` builds its own private `RestTemplate` internally
in its constructor rather than having one injected — it is still a `@Service` bean, so
callers just `@Autowired` it as usual.

### Usage example

```java
@Autowired
private LastFmService lastFmService;

LastFmResponse chart = lastFmService.getTopArtists();
List<LastFmArtist> topArtists = chart.getArtists().getArtist();
```

### Known limitation

`LastFmImage` maps the image URL field to a Java property named `text`. Last.fm's real
API response actually uses the JSON key `#text` for that field (e.g.
`{"#text": "https://...", "size": "small"}`), not `text`. Because `LastFmImage` has no
`@JsonProperty("#text")` annotation, an unmodified real Last.fm response will leave
`LastFmImage.getText()` as `null` after deserialization in production — only a response
that literally uses the key `text` (as used in this documentation's example and in the
accompanying tests) will populate it. This was observed while writing tests for this
service; it has not been fixed here since fixing it is a production code change outside
the scope of adding tests, but it is worth confirming/fixing.

### Related documentation

- [Last.fm service tests](../../tests/ExternalMusicApis/testLastFmService.md)
- [iTunes service](./itunesServiceDocumentation.md)
