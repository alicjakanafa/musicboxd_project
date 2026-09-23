## Artist Controller Tests

`ArtistControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/ArtistControllerTest.java`) is a
`@WebMvcTest` slice covering `ArtistController`
(`src/main/java/com/example/MusicBoxd/Controller/ArtistController.java`), mocking
`ArtistRepository`, `UserRepository`, `UserFavouriteArtistRepository`, `LastFmService`,
`TicketmasterService` and `ItunesService`.

### Test setup

Standard `SecurityConfig` + `TestOAuth2Config` import; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md).

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — `GET /artists/1` without authentication
  redirects to the OIDC login flow.
- **`showArtistRedirectsHomeWhenArtistNotFound`** — an unknown artist id redirects to `/`.
- **`showArtistRendersProfileWithListenersAlbumsAndConcertsForAnonymousUser`** — with
  Last.fm artist/album data, an iTunes track response and a resolvable Ticketmaster
  attraction, the model contains the listener count, top albums, popular singles (tracks
  with a preview URL, capped at 5) and concerts.
- **`showArtistMarksAsFavouriteWhenUserHasFavourited`** — `isFavourite` is `true` when
  `UserFavouriteArtistRepository.existsByUserIdAndArtistId` does, and when Last.fm/iTunes
  return nothing and Ticketmaster has no attraction id, the controller degrades to empty
  lists/`null` listeners without calling `getShowsByAttractionId`.
- **`favouriteArtistSavesFavouriteWhenNotAlreadyFavourited`** / **`...DoesNotDuplicateWhenAlreadyFavourited`**
  — `POST /artists/{id}/favourite` only inserts a `UserFavouriteArtist` row when one does
  not already exist.
- **`unfavouriteArtistDeletesTheFavourite`** — `POST /artists/{id}/unfavourite` calls
  `deleteByUserIdAndArtistId`.
- **`showArtistByNameRedirectsToExistingArtist`** / **`...CreatesArtistWhenNotFound`** —
  `GET /artists/from-name` reuses an existing artist (case-insensitively) or creates one.
- **`showAllAlbumsRedirectsHomeWhenArtistNotFound`** / **`...RendersAlbumListFromLastFm`** /
  **`...ReturnsEmptyListWhenLastFmResponseIsNull`** — `GET /artists/{id}/albums` renders
  `artist-albums` with the artist's Last.fm top albums, or an empty list when Last.fm has
  none.

### Running the tests

```
./mvnw -Dtest=ArtistControllerTest test
```
