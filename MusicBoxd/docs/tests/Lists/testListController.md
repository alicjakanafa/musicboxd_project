## List Controller Tests

`ListControllerTest`
(`src/test/java/com/example/MusicBoxd/Controller/ListControllerTest.java`) is a
`@WebMvcTest` slice that verifies `ListController`
(`src/main/java/com/example/MusicBoxd/Controller/ListController.java`)'s list CRUD,
album/song search, and "Want to Listen" endpoints.

### Test setup

`ListRepository`, `ListItemRepository`, `AlbumRepository`, `UserRepository`,
`LastFmService` and `ArtistRepository` are all mocked with `@MockitoBean`. See
[`../Security/testSupportHelpers.md`](../Security/testSupportHelpers.md) for the shared
`TestOAuth2Config` / `OidcTestUsers` helpers. `ListController` reads the current user via a
private `getCurrentUser(Authentication)` helper that requires a `DefaultOidcUser`
principal — the same object `SecurityMockMvcRequestPostProcessors.oidcLogin()` (used by
`OidcTestUsers`) installs — and looks it up by `sub` in `UserRepository`.

### What each test verifies

**Authentication / authorisation**
- Unauthenticated requests to `/lists` and `POST /lists/want-to-listen/{albumId}` redirect
  to `/oauth2/authorization/okta`.
- An authenticated Okta id with no matching `User` row returns `401 Unauthorized`
  (`returnsUnauthorizedWhenAuthenticatedOktaUserHasNoMatchingLocalUser`).
- Every list-scoped endpoint (`GET/POST /lists/{id}`, `/search`, `/albums`, `/songs`,
  `/albums/{albumId}`) returns `403 Forbidden` when the authenticated user does not own
  the target list, and never touches the mutating repositories in that case.

**`GET /lists`** — renders `placeholder-lists` with the current user's lists from
`findByUserIdOrderByCreatedAtDesc`.

**`POST /lists`** — creates a new `CUSTOM` list owned by the current user with the
submitted title/description and redirects to `/lists`.

**`GET /lists/{id}`** — renders `placeholder-list-items` with the list, its items
(`findByListIdOrderByPositionAsc`) and an `albums` map keyed by album id; throws for an
unknown list id.

**`GET /lists/{id}/search`** — renders the `placeholder-list-items :: searchResults`
fragment with `listId` and the Last.fm album matches; falls back to an empty result list
when Last.fm returns no matches (`null` response, missing `results`/`albummatches`).

**`POST /lists/{id}/albums`** — looks up the album on Last.fm by artist/title:
- when Last.fm returns a match, the saved `Album` uses Last.fm's own title, a release year
  parsed from the first four characters of `releasedate`, and the last artwork image URL;
  an existing `Artist` (matched case-insensitively) is reused rather than duplicated;
- when Last.fm's `releasedate` isn't a parseable year, the `NumberFormatException` is
  swallowed and `releaseYear` stays `null`;
- when Last.fm has no match, the submitted title/artist are used verbatim and a new
  `Artist` is created;
- the new `ListItem` is appended at `findMaxPosition(id) + 1` and the response redirects
  to `/lists/{id}`.

**`POST /lists/{id}/songs`** — appends a song `ListItem` (no album) at the next position
and redirects to `/lists/{id}`.

**`GET /lists/placeholder-list-form`** and **`GET /lists/placeholder-lists`** — render the
static placeholder form and redirect to `/lists`, respectively (both still require
authentication).

**`GET /lists/album/{id}`**
- Renders `album-profile` with the album, an `artist` (when the album has one), the
  unwrapped Last.fm album, and `alreadyInWantToListen` reflecting whether the current
  user's `WANT_TO_LISTEN` list already contains the album.
- `alreadyInWantToListen` is `false` and `ListItemRepository.existsByListIdAndAlbumId` is
  never called when the user has no `WANT_TO_LISTEN` list yet.
- An unknown album id redirects to `/lists` instead of throwing.

**`POST /lists/{id}/albums/{albumId}`** — adds an existing album to an owned list when it
is not already present (`existsByListIdAndAlbumId` is `false`); skips saving (no duplicate)
when it is already present; throws for an unknown album id; always redirects to
`/albums/{albumId}`.

**`POST /lists/want-to-listen/{albumId}`** — lazily creates the user's `WANT_TO_LISTEN`
list the first time it's needed, reuses it on subsequent calls, only appends the album
once (`existsByListIdAndAlbumId` guards against duplicates), and redirects to
`/albums/{albumId}`; throws for an unknown album id.

### Running the tests

```
./mvnw -Dtest=ListControllerTest test
```

or, as part of the full suite:

```
./mvnw test
```
