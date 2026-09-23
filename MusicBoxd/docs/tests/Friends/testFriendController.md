## Friend Controller Tests

`FriendControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/FriendControllerTest.java`) is a
`@WebMvcTest` slice covering `FriendController`
(`src/main/java/com/example/MusicBoxd/Controller/FriendController.java`)'s friends page,
user search, and the send/accept/decline/remove friend-request endpoints.

### Test setup

`FriendRepository`, `UserRepository`, and `NotificationService` are mocked with
`@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers used to authenticate requests, and
[`../Security/testGlobalControllerAdvice.md`](../Security/testGlobalControllerAdvice.md)
for why `UserRepository` must be mocked even though `FriendController` also uses it
directly.

### What each test verifies

- **`redirectsUnauthenticatedUserToOAuth2LoginForFriendsPage`** — an unauthenticated
  `GET /friends` is redirected to `/oauth2/authorization/okta` instead of reaching the
  controller.
- **`friendsPageShowsSuggestedUsersExcludingCurrentUserFriendsAndPendingRequests`** —
  `GET /friends` populates `suggestedUsers` with only users that are not the current
  user, not already friends, and not part of a pending sent/received request; accepted
  friendships where the current user is the *requester* populate `friendIds`.
  `friendsPageTreatsCurrentUserAsFriendWhenTheyAreTheReceiverOfTheAcceptedRequest`
  covers the mirrored case where the current user is the *receiver* of the accepted
  friendship.
- **`friendsPageThrowsWhenAuthenticatedPrincipalHasNoMatchingLocalUser`** — if the
  authenticated OIDC principal has no matching `User` row, the controller raises
  `RuntimeException("Current user not found")` rather than rendering a page for a
  non-existent user.
- **`searchReturnsMatchingUsersExcludingCurrentUser`** — `GET /friends/search?query=...`
  returns matching users (via `findByUsernameContainingIgnoreCase`) with the current
  user filtered out of the results.
- **`searchWithBlankQueryReturnsEmptyResultsWithoutCallingRepository`** — a blank/absent
  `query` short-circuits to an empty `searchResults` list without querying the
  repository.
- **`sendFriendRequestSavesRequestAndNotifiesReceiver`** — `POST /friends/request`
  saves a new `PENDING` `Friend` and calls `NotificationService.notifyFriendRequest`,
  then redirects to `/friends`.
- **`sendFriendRequestToSelfDoesNothing`**,
  **`sendFriendRequestToMissingReceiverDoesNothing`**,
  **`sendFriendRequestWhenRequestAlreadyExistsDoesNotDuplicate`**,
  **`sendFriendRequestWhenReverseRequestAlreadyExistsDoesNotDuplicate`** — sending a
  request to yourself, to a non-existent user, or when a request already exists in
  either direction, is a no-op (no save, no notification).
- **`acceptFriendRequestMarksAcceptedAndNotifiesRequester`** — `POST
  /friends/accept/{id}` sets the friend link's status to `ACCEPTED` and notifies the
  original requester via `NotificationService.notifyFriendAccepted`.
- **`acceptFriendRequestWhenNotFoundDoesNothing`**,
  **`acceptFriendRequestWhenCurrentUserIsNotReceiverIsRejected`**,
  **`acceptFriendRequestWhenAlreadyProcessedIsIgnored`** — accepting a missing request,
  a request addressed to someone else, or a request that is no longer `PENDING`, does
  not save or notify.
- **`declineFriendRequestMarksDeclined`** — `POST /friends/decline/{id}` sets the
  friend link's status to `DECLINED`.
- **`declineFriendRequestWhenCurrentUserIsNotReceiverIsRejected`**,
  **`declineFriendRequestWhenMissingIsIgnored`**,
  **`declineFriendRequestWhenAlreadyProcessedIsIgnored`** — the same guard conditions
  as accept, verified for decline.
- **`removeFriendDeletesFriendshipWhenCurrentUserIsRequester`** /
  **`removeFriendDeletesFriendshipWhenCurrentUserIsReceiver`** — `POST
  /friends/remove/{id}` deletes the friend link when the current user is either side of
  it.
- **`removeFriendWhenCurrentUserIsUnrelatedDoesNotDelete`**,
  **`removeFriendWhenMissingIsIgnored`** — removing a friendship the current user is not
  part of, or one that does not exist, does not delete anything.

### Running the tests

```
./mvnw -Dtest=FriendControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
