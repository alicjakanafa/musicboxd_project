## User Favourite Artist Repository Tests

`UserFavouriteArtistRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/UserFavouriteArtistRepositoryTest.java`)
is a `@DataJpaTest` covering
[`UserFavouriteArtistRepository`](../../features/Favourites/userFavouriteArtistRepositoryDocumentation.md)
and, by extension, the
[`UserFavouriteArtist`](../../features/Favourites/userFavouriteArtistModelDocumentation.md)
entity's persistence behavior.

### Test setup

Same H2/`datajpatest` setup as the other repository tests (see
[`testUserFavouriteAlbumModel`](./testUserFavouriteAlbumModel.md) for details).
`userId` and `artistId` are plain `Long` columns with no `@ManyToOne` association to
`User`/`Artist`, so the tests use plausible literal `Long` ids directly instead of
persisting real `User`/`Artist` rows first.

### What each test verifies

- **`savesUserFavouriteArtistAndGeneratesId`** — saving a new `UserFavouriteArtist`
  generates an id and round-trips `userId`/`artistId`.
- **`findByIdReturnsAllPersistedFields`** — a favourite persisted directly via
  `TestEntityManager` can be read back through `findById`.
- **`deleteByIdRemovesUserFavouriteArtist`** — after `deleteById`, the favourite can no
  longer be found by `findById`.
- **`findByUserIdOrderByIdAscReturnsOnlyThatUsersFavouritesInInsertionOrder`** —
  `findByUserIdOrderByIdAsc` only returns the given user's favourites, ordered by `id`
  ascending.
- **`findByUserIdOrderByIdAscReturnsEmptyListWhenUserHasNoFavourites`** — a user with no
  favourite artists gets back an empty list.
- **`findByUserIdAndArtistIdReturnsMatchingFavourite`** /
  **`findByUserIdAndArtistIdReturnsEmptyWhenNoMatch`** — `findByUserIdAndArtistId`
  finds the matching favourite when one exists, and returns `Optional.empty()`
  otherwise.
- **`existsByUserIdAndArtistIdReturnsTrueWhenFavouriteExists`** /
  **`existsByUserIdAndArtistIdReturnsFalseWhenFavouriteDoesNotExist`** —
  `existsByUserIdAndArtistId` correctly reports whether a favourite already exists,
  which callers can use to guard against duplicate favourites.
- **`deleteByUserIdAndArtistIdRemovesOnlyMatchingFavourite`** —
  `deleteByUserIdAndArtistId` removes only the favourite for the given user/artist
  pair, leaving that user's other favourites untouched.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema. The `USER_FAVOURITE_ARTISTS` table's `unique_user_favourite_artist`
constraint and `ON DELETE CASCADE` foreign keys
(`V24__create_user_favourite_artists.sql`) are declared in the migration only, not on
the `UserFavouriteArtist` entity, so they are not recreated in the Hibernate-generated
H2 schema and cannot be exercised by this test class.

### Running the tests

```
./mvnw -Dtest=UserFavouriteArtistRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
