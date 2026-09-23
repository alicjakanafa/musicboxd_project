## Review Controller Tests

`ReviewControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/ReviewControllerTest.java`) is a
`@WebMvcTest` slice that verifies `ReviewController`
(`src/main/java/com/example/MusicBoxd/Controller/ReviewController.java`)'s review
browsing, creation, deletion and like endpoints.

### Test setup

`ReviewRepository`, `AlbumRepository`, `ArtistRepository`, `UserRepository`,
`FriendRepository`, `NotificationService`, `LastFmService` and `LikeRepository` are all
mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers used to authenticate requests. Because
`SecurityConfig` requires authentication for every request, most tests authenticate with
`OidcTestUsers.oidcUser(...)`; a few explicitly check the unauthenticated case.

### What each test verifies

**`GET /users/{userId}/reviews`**
- Unauthenticated requests are redirected to `/oauth2/authorization/okta`.
- A valid user id renders `user-reviews` with `user`, `reviews` and an `albums` map keyed
  by album id populated for each reviewed album.
- A missing user id causes the handler to throw (no exception handler is registered for
  this `RuntimeException`, so the request fails).

**`GET /reviews/{id}`**
- Renders `reviews` with `album`, `artist` and `lastFmAlbum` model attributes when the
  album has a known artist and a successful Last.fm lookup — see the *known defect* note
  below.
- When the album has no artist, `artist` and `lastFmAlbum` are both `null` and
  `LastFmService.getAlbumInfo` is never called.
- When `LastFmService.getAlbumInfo` throws, `lastFmAlbum` is `null` and the page still
  renders successfully.
- A missing album id causes the handler to throw.

**`POST /reviews/{id}` (save review)**
- Unauthenticated requests redirect to the Okta login and never call
  `ReviewRepository.save`.
- Creating a new review (no existing review for that user/album) notifies every
  `ACCEPTED` friend of the reviewer (in either friendship direction) via
  `NotificationService.notifyFriendReviewed`, and redirects to `/profile/{userId}`.
- Updating an existing review (a prior review row exists for that user/album) updates its
  rating/content in place and does **not** query `FriendRepository` or send any
  notifications.
- A missing album, or an authenticated Okta id with no matching `User`, causes the handler
  to throw.

**`POST /reviews/{id}/delete`**
- The review owner can delete their own review; `ReviewRepository.delete` is called and
  the response redirects to `/profile/{userId}`.
- A different user attempting to delete someone else's review causes the handler to throw
  and `ReviewRepository.delete` is never called.
- A missing review id, or an authenticated Okta id with no matching `User`, causes the
  handler to throw.

**`GET /reviews`**
- Renders `all-reviews` with `reviews` (ordered via
  `findAllByOrderByCreatedAtDesc`), plus `albums` and `users` maps keyed by id.
- An empty review list still renders successfully with empty maps.

**`POST /reviews/{reviewId}/like`**
- Liking a review with no existing `Like` row saves a new `Like` and redirects to
  `/albums/{albumId}`.
- Liking a review that the user already liked deletes the existing `Like` instead
  (toggle/unlike behaviour).
- A missing review id, or an authenticated Okta id with no matching `User`, causes the
  handler to throw.

### Suspected production defect

`albumReviewPageFailsToRenderWhenLastFmLookupSucceeds` documents a defect found while
writing these tests: `ReviewController#reviewAlbum` stores the raw
`LastFmAlbumResponse` wrapper returned by `LastFmService#getAlbumInfo` directly in the
`lastFmAlbum` model attribute, instead of unwrapping it with `.getAlbum()` (the way
`ListController#getAlbum` correctly does). The `reviews.html` template expects
`lastFmAlbum` to be the unwrapped `LastFmAlbum` and reads `lastFmAlbum.image` directly, so
whenever an album has a known artist and the Last.fm lookup succeeds, rendering fails with
a `SpelEvaluationException` ("Property or field 'image' cannot be found on object of type
LastFmAlbumResponse"). The test asserts this current (broken) behaviour rather than a
successful render, so it will start failing — as a signal to update the test — once the
underlying bug is fixed.

### Running the tests

```
./mvnw -Dtest=ReviewControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
