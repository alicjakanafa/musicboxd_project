## Edit Profile Controller Tests

`EditProfileControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/EditProfileControllerTest.java`) is a
`@WebMvcTest` slice that verifies `EditProfileController`
(`src/main/java/com/example/MusicBoxd/Controller/EditProfileController.java`)'s
`GET /profile/edit` (render the edit form) and `POST /profile/edit` (apply the changes)
endpoints.

### Test setup

`UserRepository` is mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers used to authenticate requests. `/profile/edit`
requires authentication under `SecurityConfig`, so unauthenticated tests assert the redirect
to `/oauth2/authorization/okta`.

Profile picture uploads are written to the real `uploads/profile-pictures/` directory (the
path the controller hard-codes), so the upload test snapshots the directory before/after and
deletes any file it created, leaving the pre-existing runtime uploads untouched.

### What each test verifies

- **`getEditProfileUnauthenticatedRedirectsToOAuth2Login`** — no session redirects to Okta
  login.
- **`getEditProfileRedirectsHomeWhenAuthenticatedUserHasNoMatchingRecord`** — an
  authenticated OIDC principal whose `sub` has no matching `User` row redirects to `/`.
- **`getEditProfileRendersFormWithCurrentUserForAuthenticatedOwner`** — a matching user is
  exposed as the `user` model attribute and the `edit-profile` view is rendered.
- **`postUpdateProfileUnauthenticatedRedirectsToOAuth2Login`** /
  **`postUpdateProfileRedirectsHomeWhenAuthenticatedUserHasNoMatchingRecord`** — the `POST`
  endpoint enforces the same authentication/user-lookup rules as the `GET` endpoint, and
  never calls `UserRepository.save(...)` in either case.
- **`postUpdateProfileTrimsAndSavesUsernameAndBioThenRedirectsToProfile`** — a non-blank
  `username` and `bio` are trimmed and saved, and the response redirects to
  `/profile/{id}`.
- **`postUpdateProfileLeavesUsernameUnchangedWhenBlank`** — a blank/whitespace-only
  `username` is ignored (the existing username is preserved) while `bio` is still updated.
- **`postUpdateProfileWithProfilePictureUploadsFileAndSavesUrl`** — a non-empty
  `profilePicture` multipart file is written under `uploads/profile-pictures/` with a
  generated UUID filename that preserves the original extension, and the saved `User`'s
  `profilePictureUrl` is set to `/uploads/profile-pictures/<uuid>.<ext>`.
- **`postUpdateProfileWithEmptyProfilePictureDoesNotChangePictureUrl`** — an empty
  multipart file (`isEmpty() == true`) is ignored; the existing `profilePictureUrl` is left
  untouched.
- **`postUpdateProfileSwallowsIOExceptionFromFileUploadAndStillSavesUser`** — exercises the
  `catch (IOException e)` branch around the file copy. Because `MockMvc`'s multipart request
  builder only accepts `MockMultipartFile` (which can't be made to throw mid-upload), this
  test calls `EditProfileController.updateProfile(...)` directly with a mocked
  `Authentication`/`OidcUser` and a hand-rolled `MultipartFile` whose `getInputStream()`
  throws. It confirms the exception is logged and swallowed rather than propagated: the
  username update and `UserRepository.save(...)` still happen, and the method still returns
  `redirect:/profile/{id}`.

### Untested branches

Two defensive branches inside the private `getCurrentUser(Authentication)` helper - returning
`null` when `authentication` is `null`/unauthenticated, and when the principal is not an
`OidcUser` - are not covered by these tests. Both endpoints are behind
`SecurityConfig`'s `anyRequest().authenticated()` rule with `oauth2Login(...)`, so in
practice every request that reaches the controller already has an authenticated `OidcUser`
principal; these branches are unreachable via the public HTTP surface and testing them would
require invoking the private method directly (against this project's testing conventions).

### Running the tests

```
./mvnw -Dtest=EditProfileControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
