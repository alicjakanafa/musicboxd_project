## Album Repository Tests

`AlbumRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/AlbumRepositoryTest.java`)
is a `@DataJpaTest` test class covering `AlbumRepository` / `Album`
persistence behavior.

### Test setup

Like the other repository tests in this project, `AlbumRepositoryTest` runs
against an in-memory H2 database instead of the real PostgreSQL/Flyway-backed
one, via the opt-in `datajpatest` Spring profile:

- `@DataJpaTest` — boots only the JPA slice of the application context.
- `@ActiveProfiles("datajpatest")` — activates
  `src/test/resources/application-datajpatest.properties`, which points at an
  in-memory H2 datasource, disables Flyway (`spring.flyway.enabled=false`),
  and lets Hibernate generate the schema from the entity mappings
  (`spring.jpa.hibernate.ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  — tells Spring Boot to use the datasource from the active profile instead
  of auto-replacing it with its own embedded database.

This keeps the test isolated from `application.properties` /
`application-test.properties`, which still point at a local PostgreSQL
`MusicBoxd_Test` database managed by Flyway — those are unaffected by this
profile and continue to be used by other tests (e.g.
`MusicBoxdApplicationTests`).

`TestEntityManager` is injected to arrange rows directly (bypassing the
repository under test where useful), and `AlbumRepository` is injected to
perform and assert on the operation actually being tested.

### What each test verifies

Every test first persists an `Artist` (via `TestEntityManager`) to use as the
album's `artistId`, since `Album` requires an artist id but does not manage
the relationship itself.

- **`savesAlbumLinkedToPersistedArtist`** — saving a new `Album` (built with
  the field constructor) via the repository populates its auto-generated
  `id`, retains the `artistId` it was given, and auto-sets `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — an album persisted directly
  through `TestEntityManager` can be found by id via the repository, and
  every field (`externalId`, `artistId`, `title`, `releaseYear`,
  `artworkUrl`, `createdAt`) round-trips unchanged.
- **`findAllReturnsAllAlbumsRegardlessOfArtist`** — persisting two albums
  results in `findAll()` returning both, identified by title.
- **`deleteByIdRemovesAlbum`** — deleting a persisted album by id removes it;
  a subsequent `findById` returns empty.

This mirrors the pattern used by `SongRepositoryTest` for `Song`/`Album`.

### Running the tests

```
./mvnw -Dtest=AlbumRepositoryTest test
```

or as part of the full suite:

```
./mvnw test
```
