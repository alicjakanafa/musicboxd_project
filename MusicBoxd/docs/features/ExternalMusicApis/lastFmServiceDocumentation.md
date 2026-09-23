## Last.fm Service

`LastFmService` (`com.example.MusicBoxd.api.lastfm.LastFmService`) is a `@Service` that
wraps several [Last.fm API](https://www.last.fm/api) methods (top-artists chart, artist
info, artist albums, album search, and album info) so the rest of the application can
fetch artist and album metadata without talking to `RestTemplate` directly.

### What it provides

- `getTopArtists()` — fetches the current top-artists chart and returns a
  [`LastFmResponse`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmResponse.java),
  which nests a [`LastFmArtists`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmArtists.java)
  wrapper containing a `List<`[`LastFmArtist`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmArtist.java)`>`.
  Each artist has `name`, `playcount`, `listeners`, `mbid`, `url` and a
  `List<`[`LastFmImage`](../../../src/main/java/com/example/MusicBoxd/api/lastfm/LastFmImage.java)`>`
  (each image has a `text` URL and a `size`, e.g. `small`/`medium`/`large`/`extralarge`).
- `getArtistInfo(String artistName)` — fetches biography and stats for a single artist
  and returns a `LastFmArtistResponse` wrapping a `LastFmArtist` (including its nested
  `LastFmBio` with `summary`/`content`). Autocorrects misspelled artist names
  (`autocorrect=1`). As a side effect, this method fetches the same URL **twice** — once
  as a raw `String` (logged to stdout for debugging) and once deserialized into
  `LastFmArtistResponse` — so callers should expect two HTTP calls per invocation.
- `getArtistAlbums(String artistName)` — fetches an artist's top albums (up to 50,
  autocorrected) and returns a `LastFmTopAlbumsResponse` wrapping a list of
  `LastFmTopAlbum`.
- `searchAlbums(String query)` — searches for albums by name (up to 20 matches) and
  returns a `LastFmSearchResponse` wrapping a list of `LastFmSearchAlbum`. If the remote
  call fails for any reason (e.g. a non-2xx response), the exception is caught, logged to
  stdout, and `null` is returned instead of propagating the exception.
- `getAlbumInfo(String artist, String album)` — fetches full album details, including
  the tracklist (`LastFmAlbum.tracks`), and returns a `LastFmAlbumResponse`. Like
  `searchAlbums`, any exception from the remote call (including a `404` for an unknown
  album) is caught, logged to stdout, and `null` is returned instead of propagating.

### Request shape

| Param         | `getTopArtists`         | `getArtistInfo`     | `getArtistAlbums`     | `searchAlbums`  | `getAlbumInfo`      |
|---------------|--------------------------|----------------------|------------------------|------------------|----------------------|
| `method`      | `chart.gettopartists`    | `artist.getinfo`     | `artist.gettopalbums`  | `album.search`   | `album.getinfo`      |
| `artist`      | —                        | the artist name      | the artist name        | —                | the artist name      |
| `album`       | —                        | —                    | —                      | the query        | the album name       |
| `api_key`     | the configured API key   | the configured key   | the configured key     | the configured key | the configured key |
| `format`      | `json`                   | `json`               | `json`                 | `json`           | `json`               |
| `limit`       | `40`                     | —                    | `50`                   | `20`             | —                    |
| `autocorrect` | —                        | `1`                  | `1`                    | —                | `1`                  |

All requests go to `https://ws.audioscrobbler.com/2.0/`.

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

LastFmArtistResponse info = lastFmService.getArtistInfo("Radiohead");
LastFmTopAlbumsResponse albums = lastFmService.getArtistAlbums("Radiohead");
LastFmSearchResponse results = lastFmService.searchAlbums("OK Computer");
LastFmAlbumResponse albumInfo = lastFmService.getAlbumInfo("Radiohead", "OK Computer");
```

### Notes on error handling and logging

- `LastFmImage` maps its image URL field to the JSON key `#text` via
  `@JsonProperty("#text")`, matching the real Last.fm API's response shape (e.g.
  `{"#text": "https://...", "size": "small"}`).
- `searchAlbums` and `getAlbumInfo` swallow any exception thrown by the underlying
  `RestTemplate` call (including HTTP error responses such as a `404` for an unknown
  album) and return `null` instead of propagating it, after logging the error message to
  stdout. Callers must be prepared to handle a `null` result from these two methods.
- `getArtistInfo` makes two HTTP requests per call — one to log the raw JSON response to
  stdout for debugging, and a second to actually deserialize the response — so it is
  twice as expensive as the other methods here.

### Related documentation

- [Last.fm service tests](../../tests/ExternalMusicApis/testLastFmService.md)
- [iTunes service](./itunesServiceDocumentation.md)
