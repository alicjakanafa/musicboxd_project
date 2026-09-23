## iTunes Service

`ItunesService` (`com.example.MusicBoxd.api.itunes.ItunesService`) is a `@Service` that
wraps the public [iTunes Search API](https://performance-partners.apple.com/search-api)
so the rest of the application can search for albums and tracks without talking to
`RestTemplate` directly.

### What it provides

- `searchAlbums(String searchTerm)` — searches for albums matching `searchTerm` and
  returns an [`ItunesAlbumResponse`](../../../src/main/java/com/example/MusicBoxd/api/itunes/ItunesAlbumResponse.java)
  containing a `resultCount` and a list of [`ItunesAlbum`](../../../src/main/java/com/example/MusicBoxd/api/itunes/ItunesAlbum.java)
  results (`collectionId`, `collectionName`, `artistName`, `artworkUrl100`,
  `artworkUrl600`, `releaseDate`, `primaryGenreName`).
- `searchTracks(String searchTerm)` — searches for individual tracks matching
  `searchTerm` and returns an `ItunesTrackResponse` containing a `resultCount` and a
  list of [`ItunesTrack`](../../../src/main/java/com/example/MusicBoxd/api/itunes/ItunesTrack.java)
  results (`trackId`, `trackName`, `artistName`, `collectionName`, `artworkUrl100`,
  `previewUrl`, `trackNumber`, `trackTimeMillis`).
- `searchAlbumsByArtist(String artistName)` — like `searchAlbums`, but requests up to
  `200` results instead of `20`, for callers that want an artist's full discography
  rather than a short list of best matches.
- `getDailyAlbum(Long userId)` — picks a "random" album for the day by hashing the
  current `LocalDate` with `userId` (when a `userId` is supplied) into a seed for a
  `java.util.Random`. The seed selects one of nine hardcoded genre search terms (`pop`,
  `rock`, `indie`, `alternative`, `jazz`, `hip hop`, `electronic`, `country`, `r&b`),
  fetches up to 50 albums for that term, and then uses the same `Random` instance to
  pick one album from the results. Because the seed is derived from the current date,
  the **same user gets the same album for the whole day**, and a different day (or a
  request with no `userId`, which falls back to `LocalDate.now().toEpochDay()`) produces
  a different seed. Returns `null` if the search returns no results (empty or missing
  `results`, or an empty response body).
- `getAlbumTracks(Long collectionId)` — looks up the track listing for an album by its
  iTunes `collectionId` and returns an `ItunesTrackResponse`.

### Request shape

`searchAlbums`, `searchTracks`, `searchAlbumsByArtist`, and `getDailyAlbum` all build a
GET request to `https://itunes.apple.com/search` with the following query parameters:

| Param    | `searchAlbums`  | `searchTracks`   | `searchAlbumsByArtist` | `getDailyAlbum`         |
|----------|-----------------|------------------|-------------------------|--------------------------|
| `term`   | the search term | the search term  | the artist name         | a random genre keyword   |
| `media`  | `music`         | `music`          | `music`                 | `music`                   |
| `entity` | `album`         | `song`           | `album`                 | `album`                   |
| `limit`  | `20`            | `20`             | `200`                   | `50`                       |

`getAlbumTracks(collectionId)` instead builds a GET request to
`https://itunes.apple.com/lookup` with `id=<collectionId>` and `entity=song`.

If no results match, iTunes returns `resultCount: 0` and an empty `results` list, which
is passed straight through — the service does not throw or substitute a default value.

### The `text/javascript` content-type quirk

The iTunes Search API responds with `Content-Type: text/javascript` (not
`application/json`), even though the body is plain JSON. Spring's default JSON message
converter only accepts `application/json`-family content types, so without extra setup
`RestTemplate` would reject the response with an `UnknownContentTypeException`.

To work around this, `ItunesService`'s constructor walks the `RestTemplate`'s configured
message converters, finds the Jackson JSON converter, and adds `text/javascript` to its
list of supported media types. This happens once, when the service (and its private
internal `RestTemplate`) is constructed — it is not configurable and does not depend on
any application property.

### Construction

Unlike most services in this project, `ItunesService` has a no-args constructor and
builds its own private `RestTemplate` internally rather than having one injected. It is
still a `@Service` bean managed by Spring, so callers just `@Autowired` it as usual —
the internal `RestTemplate` is an implementation detail.

### Usage example

```java
@Autowired
private ItunesService itunesService;

ItunesAlbumResponse albums = itunesService.searchAlbums("Radiohead");
ItunesTrackResponse tracks = itunesService.searchTracks("Karma Police");
ItunesAlbum todaysAlbum = itunesService.getDailyAlbum(currentUserId);
ItunesTrackResponse albumTracks = itunesService.getAlbumTracks(todaysAlbum.getCollectionId());
```

### Related documentation

- [iTunes service tests](../../tests/ExternalMusicApis/testItunesService.md)
- [Last.fm service](./lastFmServiceDocumentation.md)
