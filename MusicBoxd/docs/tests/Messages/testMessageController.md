## Message Controller Tests

`MessageControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/MessageControllerTest.java`) covers
`MessageController`
(`src/main/java/com/example/MusicBoxd/Controller/MessageController.java`)'s conversation
list, a single conversation thread, and sending a new message, including the
friends-only gating enforced by `FriendRepository`.

### Test setup

`MessageRepository`, `UserRepository`, `NotificationRepository`, and
`FriendRepository` are mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers, and
[`../Security/testGlobalControllerAdvice.md`](../Security/testGlobalControllerAdvice.md)
for why `UserRepository` must be mocked in every `@WebMvcTest`.

Unlike most controllers in this codebase, `MessageController` reads its principal
directly from `SecurityContextHolder` (field-injected, not via an `Authentication`
parameter), and its two `GET` handlers render Thymeleaf templates
(`messages/list`, `messages/index`) that do not currently exist under
`src/main/resources/templates` (see "Suspected production defect" below). Driving
those handlers through `MockMvc` fails at view resolution rather than exercising the
controller's own logic, so those specific tests `@Autowired` the `MessageController`
bean directly (still wired to the same mocked repositories), populate
`SecurityContextHolder` with a hand-built `DefaultOidcUser` via a small
`authenticateAs(sub)` helper, and call the handler method directly with a plain
`ExtendedModelMap`. The security-redirect test and the `POST /messages/{receiverId}`
tests (which return `RedirectView`, not a template) still go through `MockMvc` normally.

### What each test verifies

- **`redirectsUnauthenticatedUserToOAuth2LoginForMessageList`** — an unauthenticated
  `GET /messages` is redirected to `/oauth2/authorization/okta`.
- **`messageListShowsOneConversationPerOtherUserWithLatestMessage`** — `messages(...)`
  collapses all of the current user's messages into one conversation per other user,
  keyed by that user's id, keeping only the most recent message per conversation.
- **`messageListThrowsWhenCurrentUserHasNoMatchingLocalRecord`** — if the authenticated
  principal has no matching `User` row, `messages(...)` throws
  `IllegalStateException("User not found in local database")`.
- **`conversationIsShownWhenUsersAreFriends`** — `index(...)` (`GET
  /messages/{receiverId}`) returns the full message thread with the given user,
  the `receiver`, and a fresh `submittedMessage` for the reply form, when the two
  users are accepted friends (checked in either direction).
- **`conversationRedirectsToFriendsWithPendingMessageWhenRequestIsPending`** — if the
  users are not yet friends but have a pending friend request, the response redirects
  to `/friends?message=pending` instead of showing the thread.
- **`conversationRedirectsToFriendsWhenUsersAreNotFriendsAtAll`** — with no friendship
  or pending request in either direction, the response redirects to `/friends`.
- **`conversationThrowsWhenReceiverDoesNotExist`** — if `receiverId` does not match a
  `User`, `index(...)` throws `IllegalStateException("Receiver not found")`.
- **`createSavesMessageAndNotificationWhenUsersAreFriends`** — `POST
  /messages/{receiverId}` with friends saves a `Message` (unread, current timestamp)
  and a `NEW_MESSAGE` `Notification` addressed to the receiver, then redirects back to
  the conversation.
- **`createRedirectsToFriendsWhenUsersAreNotFriends`** — sending to a non-friend
  redirects to `/friends` without saving a message or notification.
- **`createRedirectsBackToConversationWhenMessageAndSongAreBothEmpty`** — submitting
  with blank content and no song redirects back to the conversation without saving.
- **`createSavesSongDetailsWithBlankContentWhenSongIsShared`** — sharing a song with no
  text content saves the song's title/artist/image/preview URL with an empty `content`
  string.
- **`createThrowsWhenReceiverDoesNotExist`** — posting to a non-existent `receiverId`
  throws `IllegalStateException("Receiver not found")` and does not save a message.

### Suspected production defect

`MessageController.messages()` and `MessageController.index()` return the view names
`"messages/list"` and `"messages/index"`, but no corresponding Thymeleaf templates
exist under `src/main/resources/templates` (there is no `templates/messages`
directory at all). As implemented, both `GET` endpoints would fail at runtime with a
`TemplateInputException` once a request reaches view rendering, even though the
controller's own logic (model population, friend-gating, redirects) is correct. This
was not fixed as part of this test change, per instructions not to modify production
code; the tests above call the controller methods directly to verify the logic while
avoiding the missing-template failure.

### Running the tests

```
./mvnw -Dtest=MessageControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
