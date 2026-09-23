## Profile Controller Tests

`ProfileControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/ProfileControllerTest.java`) is a
`@WebMvcTest` slice that verifies `ProfileController`
(`src/main/java/com/example/MusicBoxd/Controller/ProfileController.java`)'s three endpoints:
`GET /profile/{id}` (the main profile page), `GET /placeholder-list-form`, and
`GET /profile/{id}/favourite-artists`.

### Test setup

Every repository/service the controller depends on is mocked with `@MockitoBean`:
`UserRepository`, `ReviewRepository`, `AlbumRepository`, `ArtistRepository`,
`UserFavouriteAlbumRepository`, `UserFavouriteArtistRepository`, `FriendRepository`,
`ListRepository`, `ListItemRepository`, `LastFmService`, and `SpotifyController` (mocked as
a plain collaborator - its own HTTP calls are exercised separately in
`SpotifyControllerTest`, if present, and are irrelevant here). See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers used to authenticate requests. `/profile/**` is
not in `SecurityConfig`'s public allow-list, so every test either authenticates with
`OidcTestUsers.oidcUser(...)` or asserts the unauthenticated redirect to
`/oauth2/authorization/okta`.

A `stubEmptyProfileCollaborators(id)` helper stubs every collaborator repository to return
an empty result for a given profile id, used as a baseline by tests that only care about one
part of the page. Tests that also need a favourites/review-specific stub call
`stubEmptyProfileCollaborators` **before** overriding the specific stub they care about,
since Mockito's last matching `when(...)` for the same arguments wins.

### What each test verifies

**`GET /profile/{id}`**

- **`unauthenticatedRequestToProfileRedirectsToOAuth2Login`** — no session redirects to Okta
  login and never touches `UserRepository`.
- **`redirectsHomeWhenProfileUserDoesNotExist`** — an unknown id redirects to `/`.
- **`rendersProfileWithReviewsAlbumsAndDatabaseArtworkSkippingReviewsWithoutUsableAlbums`** —
  a review whose album already has `artworkUrl` in the database is rendered using that
  artwork without ever consulting `ArtistRepository`/`LastFmService`; a review pointing at an
  album id that no longer exists is silently skipped.
- **`fetchesArtworkFromLastFmSkippingBlankImagesWhenDatabaseArtworkIsMissing`** — for a
  favourite album with no stored artwork, the controller looks up the artist, calls
  `LastFmService.getAlbumInfo(...)`, skips blank `LastFmImage` entries, uses the first
  non-blank one, and persists it back onto the `Album` via `AlbumRepository.save(...)`.
- **`getArtworkReturnsNullWhenAlbumHasNoArtistId`**,
  **`getArtworkReturnsNullWhenArtistNotFound`**,
  **`getArtworkReturnsNullWhenLastFmHasNoAlbum`**,
  **`getArtworkReturnsNullWhenLastFmImagesAreEmpty`**,
  **`getArtworkReturnsNullAndIsSwallowedWhenLastFmThrows`** — the artwork-lookup fallback
  chain returns `null` (and never calls `AlbumRepository.save(...)`) for each failure mode:
  missing artist id, unknown artist, no Last.fm album, no Last.fm images, and an exception
  thrown by `LastFmService` (which is caught and logged, not propagated).
- **`reviewWithoutAlbumIdNeverTriggersAnAlbumLookup`** — a review with a `null` album id
  (e.g. a song-only review) is skipped by the album/artwork-building loop without any
  `AlbumRepository.findById(...)` call. *(See "Known risk" below.)*
- **`computesFollowerAndFollowingCountsAcrossFriendshipStatuses`** — follower/following
  counts are derived by scanning every `Friend` row: `ACCEPTED` friendships count towards
  both; `PENDING` counts towards `followerCount` only when the profile user is the receiver
  and towards `followingCount` only when they are the requester; `REJECTED` and null-status
  rows are ignored; rows with a null requester/receiver id are ignored.
- **`buildsFollowingUsersListFromAcceptedFriendshipsInEitherDirection`** — the
  `followingUsers` model attribute resolves the *other* party for each `ACCEPTED`
  friendship, regardless of whether the profile user was the requester or the receiver.
- **`listsAndCountsAreExposedOnTheModel`** — `favouriteArtistCount`, `profileLists`,
  `profileListCount`, and (indirectly) `profileListItemCounts` reflect the repositories'
  data.
- **`invalidArtistRangeAndTopTypeFallBackToDefaults`** — unrecognised `artistRange`/`topType`
  query parameters fall back to `medium_term`/`artists`.
- **`spotifyNotConnectedWhenNoAccessTokenInSession`** — with no `spotifyAccessToken` in the
  session, `spotifyConnected` is `false` and `SpotifyController.getSpotifyData(...)` is never
  called.
- **`spotifyConnectedLoadsTopArtistsWhenTokenPresentAndTopTypeArtists`** /
  **`spotifyConnectedLoadsTopTracksWhenTopTypeTracks`** — with a session token present, the
  controller delegates to `SpotifyController` and exposes `topArtists`/`topTracks` and
  `spotifyConnected = true` depending on `topType`.
- **`spotifyConnectionFailureIsCaughtAndMarksSpotifyDisconnected`** — an exception from
  `SpotifyController.getSpotifyData(...)` is caught and reported as `spotifyConnected = false`
  rather than failing the request.

**`GET /placeholder-list-form`**

- **`placeholderListFormRequiresAuthentication`** / **`placeholderListFormRendersForAuthenticatedUser`**
  — authentication is required; an authenticated request renders the `placeholder-list-form`
  view.

**`GET /profile/{id}/favourite-artists`**

- **`favouriteArtistsPageUnauthenticatedRedirectsToOAuth2Login`**,
  **`favouriteArtistsPageRedirectsHomeWhenUserMissing`**,
  **`favouriteArtistsPageRendersArtistsForExistingUser`** — mirror the main profile page's
  authentication/not-found handling and confirm the `user`, `favouriteArtists`, and
  `favouriteArtistCount` model attributes.

### Known risk (not a test defect)

`profile-page.html` evaluates `lastFmArtwork[review.albumId]` using Thymeleaf/SpEL
map-index syntax rather than `Map.get(...)`. During one full-suite run this was observed to
throw `IllegalStateException: No index` when `review.albumId` is `null` (e.g. a song-only
review) - `Map.get(null)` on a `HashMap` is safe, but SpEL's map indexer is not always. The
failure did not reproduce reliably in isolation (it appears related to Spring's SpEL
compiler warming up after many evaluations), so
`reviewWithoutAlbumIdNeverTriggersAnAlbumLookup` intentionally verifies only the
controller-level contract (no album lookup for a null album id) and tolerates either
rendering outcome. This is flagged as a suspected template defect worth a follow-up, not
something this test suite works around silently.

### Running the tests

```
./mvnw -Dtest=ProfileControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
