## Album Controller Tests

`AlbumControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/AlbumControllerTest.java`) is a
`@WebMvcTest` slice covering `AlbumController`
(`src/main/java/com/example/MusicBoxd/Controller/AlbumController.java`), with every
repository/service it depends on (`AlbumRepository`, `ArtistRepository`,
`UserFavouriteAlbumRepository`, `UserRepository`, `LastFmService`, `ItunesService`,
`ReviewRepository`, `LikeRepository`, `NotificationService`, `ListRepository`,
`ListItemRepository`) replaced with `@MockitoBean`s.

### Test setup

`SecurityConfig` and `TestOAuth2Config` are imported as usual; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`OidcTestUsers`/`TestOAuth2Config` helpers. `GlobalControllerAdvice` also runs for every
request, so `UserRepository` is always mocked even for tests that don't otherwise need it.

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — an unauthenticated request to `/albums/save`
  is redirected to the OIDC login flow instead of reaching the controller.
- **`saveItunesAlbumRedirectsToExistingAlbumWhenExternalIdAlreadySaved`** /
  **`saveItunesAlbumCreatesArtistAndAlbumWhenNew`** /
  **`saveItunesAlbumHandlesBlankReleaseDate`** — `GET /albums/save` reuses an already
  imported album by external id, otherwise creates the artist (if missing) and album,
  parsing the release year from the supplied date or leaving it `null` when blank.
- **`showAlbumRedirectsHomeWhenAlbumNotFound`** / **`...WhenArtistIdMissing`** /
  **`...WhenArtistNotFound`** — `GET /albums/{id}` redirects to `/` for a missing album, an
  album with no `artistId`, or an artist id that no longer resolves.
- **`showAlbumRendersDetailsWithoutTrackPreviewsWhenNoUserRecordExists`** — renders
  `album-profile` with a `null` `lastFmAlbum`, no track previews, and `alreadyInWantToListen
  = false` when Last.fm has no data and the signed-in principal has no matching `User` row.
- **`showAlbumIncludesLastFmAlbumAndTrackPreviewsAndWantToListenFlagForSignedInUser`** —
  with a full user/session, the model includes the Last.fm album, iTunes track previews
  keyed by track name, `alreadyInWantToListen = true` when the album is already in the
  user's Want to Listen list, and the reviews/like counts for that album.
- **`toggleReviewLikeAddsLikeAndNotifiesReviewOwner`** / **`...RemovesExistingLikeAndSkipsNotificationForOwnReview`**
  — liking a review creates a `Like` and notifies the review's author (unless liking your
  own review), while liking again removes the existing `Like` without notifying.
- **`addFavouriteAlbumReplacesExistingFavouriteAtSamePosition`** — favouriting an album
  removes any existing favourite for that album and any favourite already occupying the
  requested position before saving the new one.
- **`removeFavouriteAlbumDeletesExistingFavourite`** — unfavouriting deletes the matching
  `UserFavouriteAlbum` row.

### Running the tests

```
./mvnw -Dtest=AlbumControllerTest test
```
