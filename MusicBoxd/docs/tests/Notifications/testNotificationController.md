## Notification Controller Tests

`NotificationControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/NotificationControllerTest.java`) is a
`@WebMvcTest` slice covering `NotificationController`
(`src/main/java/com/example/MusicBoxd/Controller/NotificationController.java`)'s
notifications list and "open notification" (mark-as-read + redirect) endpoints.

### Test setup

`NotificationRepository` and `UserRepository` are mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers, and
[`../Security/testGlobalControllerAdvice.md`](../Security/testGlobalControllerAdvice.md)
for why `UserRepository` must be mocked in every `@WebMvcTest`.

### What each test verifies

- **`redirectsUnauthenticatedUserToOAuth2LoginForNotificationsPage`** — an
  unauthenticated `GET /notifications` is redirected to
  `/oauth2/authorization/okta`.
- **`notificationsPageShowsListAndUnreadCountForCurrentUser`** — `GET /notifications`
  populates `notifications` (via `findByUserIdOrderByCreatedAtDesc`) and
  `unreadNotificationCount` (via `countByUserIdAndReadFalse`) for the current user.
- **`notificationsPageRedirectsHomeWhenCurrentUserCannotBeFound`** — if the
  authenticated principal has no matching local `User`, the page redirects to `/`
  without querying notifications.
- **`openingFriendRequestNotificationMarksReadAndRedirectsToActorProfile`** /
  **`openingFriendAcceptedNotificationRedirectsToActorProfile`** — opening a
  `FRIEND_REQUEST` or `FRIEND_ACCEPTED` notification marks it read and redirects to
  `/profile/{actorId}`.
- **`openingAlbumReviewedNotificationRedirectsHome`** — opening an `ALBUM_REVIEWED`
  notification redirects to `/`.
- **`openingUnknownNotificationTypeRedirectsToNotificationsList`** — any other
  notification type redirects back to `/notifications`.
- **`openingNotificationThatBelongsToAnotherUserThrows`** — opening a notification
  addressed to a different user throws
  `RuntimeException("You cannot open another user's notification")` and does not mark
  it read.
- **`openingMissingNotificationThrows`** — opening a non-existent notification id
  throws `RuntimeException("Notification not found")`.
- **`openingNotificationRedirectsHomeWhenCurrentUserCannotBeFound`** — if the
  authenticated principal has no matching local `User`, opening a notification
  redirects to `/` without looking up the notification.

### Running the tests

```
./mvnw -Dtest=NotificationControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
