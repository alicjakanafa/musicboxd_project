## User Controller Tests

`UserControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/UserControllerTest.java`) is a
`@WebMvcTest` slice that verifies `UserController`
(`src/main/java/com/example/MusicBoxd/Controller/UserController.java`)'s
`GET /users/after-login` endpoint - the handler Okta's OAuth2 login success handler
redirects to (see
[`../Security/testSecurityConfig.md`](../Security/testSecurityConfig.md)) - correctly finds
or creates the signed-in user's `User` row and default lists.

### Test setup

`UserRepository` and `ListRepository` are mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers used to authenticate requests, and for why the
`okta.oauth2.issuer` test property and excluded auto-configurations are needed.

### What each test verifies

- **`redirectsUnauthenticatedUserToOAuth2LoginInsteadOfProcessingAfterLogin`** — an
  unauthenticated request to `/users/after-login` is redirected to
  `/oauth2/authorization/okta` instead of reaching the controller; neither
  `UserRepository.save(...)` nor `ListRepository.save(...)` are called.
- **`redirectsToHomeAndDoesNotRecreateAnAlreadyExistingUser`** — when
  `UserRepository.findByOktaUserId(...)` already returns a matching `User`, the response
  redirects to `/` and neither repository's `save(...)` is called (no duplicate `User` or
  lists are created for a returning user).
- **`createsUserAndDefaultListsWhenNoMatchingUserExistsYet`** — when no matching `User`
  exists yet, the controller: saves a new `User` populated from the OIDC principal's `sub`
  (as `oktaUserId`) and `email` (as both `email` and `username`); then creates exactly two
  default `List`s for that user's generated id - one `WANT_TO_LISTEN` titled "Want to
  Listen" and one `FAVOURITES` titled "Favorites" - via `ListRepository.save(...)`; and
  finally redirects to `/`.

### Running the tests

```
./mvnw -Dtest=UserControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
