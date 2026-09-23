## User Favourite Album Repository Tests

`UserFavouriteAlbumRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/UserFavouriteAlbumRepositoryTest.java`)
is a `@DataJpaTest` covering
[`UserFavouriteAlbumRepository`](../../features/Favourites/userFavouriteAlbumRepositoryDocumentation.md)
and, by extension, the
[`UserFavouriteAlbum`](../../features/Favourites/userFavouriteAlbumModelDocumentation.md)
entity's persistence behavior.

### Test setup

The class runs against an in-memory H2 database instead of the project's normal
PostgreSQL/Flyway-managed database, so it stays fast and does not require a running
Postgres instance:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at H2,
  disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate generate the
  schema from the JPA entity mappings (`ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` keeps
  `@DataJpaTest` from swapping in its own default embedded datasource, so the explicit
  H2 profile config above is the one actually used.

`userId` and `albumId` are plain `Long` columns with no `@ManyToOne` association to
`User`/`Album`, so the tests use plausible literal `Long` ids directly instead of
persisting real `User`/`Album` rows first.

### What each test verifies

- **`savesUserFavouriteAlbumAndGeneratesId`** — saving a new `UserFavouriteAlbum`
  through `UserFavouriteAlbumRepository.save` generates an id and round-trips
  `userId`/`albumId`/`position`.
- **`findByIdReturnsAllPersistedFields`** — a favourite persisted directly via
  `TestEntityManager` can be read back through `findById`, with all fields intact.
- **`deleteByIdRemovesUserFavouriteAlbum`** — after `deleteById`, the favourite can no
  longer be found by `findById`.
- **`findByUserIdOrderByPositionAscReturnsOnlyThatUsersFavouritesInPositionOrder`** —
  `findByUserIdOrderByPositionAsc` only returns the given user's favourites (excluding
  another user's), sorted by `position` ascending regardless of insertion order.
- **`findByUserIdOrderByPositionAscReturnsEmptyListWhenUserHasNoFavourites`** — a user
  with no favourites gets back an empty list, not `null` or an error.
- **`findByUserIdAndAlbumIdReturnsMatchingFavourite`** /
  **`findByUserIdAndAlbumIdReturnsEmptyWhenNoMatch`** — `findByUserIdAndAlbumId` finds
  the matching favourite when one exists for that user/album pair, and returns
  `Optional.empty()` otherwise.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema. The `top_four` table's `unique_user_album` and `unique_user_position`
constraints (`V20__create_favourite_albums.sql`,
`V22__rename_user_favourite_albums.sql`) are declared in the migration only, not on the
`UserFavouriteAlbum` entity (no `@Table(uniqueConstraints = ...)`), so they are not
recreated in the Hibernate-generated H2 schema and cannot be exercised by this test
class. There is also no test for the migration's foreign keys to `users`/`albums`,
since the entity has no `@ManyToOne` association enforcing them at the JPA level.

### Running the tests

```
./mvnw -Dtest=UserFavouriteAlbumRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
