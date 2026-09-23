## Home Controller Tests

`HomeControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/HomeControllerTest.java`) is a
`@WebMvcTest` slice covering `HomeController`
(`src/main/java/com/example/MusicBoxd/Controller/HomeController.java`), the controller
behind the `/` landing page and the `/profile` redirect. `LastFmService`,
`SpotifyController`, `ItunesService`, `UserRepository`, `NotificationRepository`,
`FriendRepository`, `ReviewRepository` and `AlbumRepository` are all `@MockitoBean`s.

### Test setup

Standard `SecurityConfig` + `TestOAuth2Config` import; see
[`testSupportHelpers.md`](../Security/testSupportHelpers.md). `SpotifyController` is mocked
directly (it's a concrete `@Controller`, not an interface) since `HomeController` calls its
`getSpotifyData(...)` helper when a Spotify access token is present in the session.

### What each test verifies

- **`redirectsUnauthenticatedUserToLogin`** — `GET /` without authentication redirects to
  the OIDC login flow.
- **`indexShowsTopTenArtistsAndSuggestedAlbumForAnonymousLookupWhenUserMissing`** — when the
  signed-in principal has no matching `User` row, the page still renders with the top ten
  Last.fm artists and the daily suggested album (looked up with a `null` user id), but no
  `notifications` attribute is added.
- **`indexShowsNotificationsAndFriendActivityForSignedInUser`** — for a real `User`, the
  model includes the latest 6 notifications, the unread count, `currentUser`, and the
  signed-in user's friends' recent reviews joined with the reviewers and their albums'
  artwork.
- **`indexIncludesSpotifyDataWhenSessionHasAnAccessToken`** — when the session holds a
  `spotifyAccessToken`, `SpotifyController.getSpotifyData(...)` is invoked to populate the
  Spotify widgets.
- **`indexTruncatesFriendReviewsToTenAndSkipsReviewsWithMissingAlbums`** — more than 10
  friend reviews are truncated to the 10 most recent, friendship direction is resolved
  whether the current user is the requester or the receiver, and reviews whose album can no
  longer be found are skipped when building `friendReviewAlbums`.
- **`profileRedirectsToOwnProfilePageWhenSignedIn`** — `GET /profile` redirects to
  `/profile/{id}` for the current user.
- **`profileThrowsWhenAuthenticatedPrincipalHasNoMatchingUserRecord`** — documents that
  `GET /profile` propagates a `RuntimeException` (surfacing as a `ServletException` in the
  test, and as an unhandled 500 in production, since there is no `@ExceptionHandler` for it)
  when the authenticated principal has no corresponding `User` row.

### Running the tests

```
./mvnw -Dtest=HomeControllerTest test
```

### Known limitation / suspected production defect

The `index` template (`src/main/resources/templates/index.html`) evaluates
`friendReviewAlbums[review.albumId] != null` for every friend review. If a friend review has
a `null` `albumId` (the `Review` model allows reviews of either an album or a song via
nullable `albumId`/`songId` columns), Spring's SpEL map-indexer throws
`IllegalStateException: No index` because it does not accept a `null` index, which surfaces
as a 500 error when rendering `/`. This was discovered while writing
`HomeControllerTest` and reproduces with `@WebMvcTest`'s real Thymeleaf rendering; it was not
fixed here since production code changes are out of scope for this test-writing task.
`indexTruncatesFriendReviewsToTenAndSkipsReviewsWithMissingAlbums` therefore exercises the
"album no longer found" branch using reviews with a non-`null` `albumId` instead, so the
`albumId == null` branch of `HomeController#loadFriendsActivity` remains uncovered by this
test class.
