## Itunes Controller Tests

`ItunesControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/ItunesControllerTest.java`) is a
`@WebMvcTest` slice covering `ItunesController`
(`src/main/java/com/example/MusicBoxd/Controller/ItunesController.java`), the controller
that drives album/track search and imports albums (and their songs) discovered through
iTunes or Last.fm into the local `Album`/`Artist`/`Song` tables. `ItunesService`,
`LastFmService`, `ArtistRepository`, `AlbumRepository` and `SongRepository` are all
`@MockitoBean`s; no real HTTP calls are made.

### Test setup

Standard `SecurityConfig` + `TestOAuth2Config` import; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md).

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — `GET /search` without authentication redirects
  to the OIDC login flow.
- **`searchAlbumsReturnsMatchesWhenLastFmHasResults`** / **`...ReturnsEmptyListWhenLastFmResponseIsNull`**
  — `GET /search` renders `album-search` with Last.fm's album matches, or an empty list when
  Last.fm returns nothing.
- **`searchTracksReturnsResultsFromItunes`** — `GET /search/tracks` renders `track-search`
  with the iTunes track results.
- **`saveAlbumCreatesNewArtistAndAlbumWhenNoneExist`** / **`...UpdatesExistingAlbumInsteadOfCreatingDuplicate`**
  / **`...IgnoresUnparseableReleaseDate`** — `GET /album/save` creates the artist/album on
  first import, updates the existing row (title, artist, artwork, release year) on a repeat
  import instead of duplicating it, and leaves the release year `null` when the supplied
  date can't be parsed.
- **`saveAlbumSavesNewSongsFromItunesTracksWithoutDuplicates`** — importing an album's
  tracks skips tracks with no `trackId`, skips tracks whose external id is already saved,
  and saves exactly the new ones.
- **`getAlbumFromArtistReturnsExistingAlbumWhenTitleAlreadySaved`** — reuses an
  already-imported album matched by artist + title (case-insensitively) without touching
  Last.fm.
- **`getAlbumFromArtistBackfillsArtworkWhenExistingAlbumHasNone`** /
  **`...BackfillsArtworkFromLastAvailableImageWhenEarlierEntriesAreBlank`** /
  **`...LeavesArtworkNullWhenLastFmAlbumHasNoImages`** — when an existing album has no
  artwork, the controller fetches it from Last.fm, preferring the last non-blank image URL
  in the list, and leaves artwork untouched (and does not re-save) if Last.fm has no usable
  image.
- **`getAlbumFromArtistSwallowsExceptionsWhileFetchingArtworkAndStillRedirects`** — a
  `LastFmService` failure while looking up artwork is swallowed so album creation still
  succeeds.
- **`getAlbumFromArtistCreatesNewAlbumWhenNotFound`** — creates a new artist/album (with a
  synthetic `lastfm:` external id) when nothing matches yet.

### Running the tests

```
./mvnw -Dtest=ItunesControllerTest test
```

### Known limitation

`ItunesController.normalise(String)` is a private helper that is never called from anywhere
in the class or the rest of the codebase; it is effectively dead code and is not exercised
by these tests, since testing private methods directly is out of scope and there is no
public entry point that reaches it.
