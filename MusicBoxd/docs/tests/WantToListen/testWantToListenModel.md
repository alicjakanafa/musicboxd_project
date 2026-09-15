## Want To Listen Repository Tests

`WantToListenRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/WantToListenRepositoryTest.java`) is a
`@DataJpaTest` covering [`WantToListenRepository`](../../features/WantToListen/wantToListenRepositoryDocumentation.md)
and, by extension, the
[`WantToListen`](../../features/WantToListen/wantToListenModelDocumentation.md) entity's
persistence behavior.

### Test setup

The class runs against an in-memory H2 database instead of the project's normal
PostgreSQL/Flyway-managed database, so it stays fast and does not require a running
Postgres instance. This is opt-in and isolated from the rest of the suite:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at H2,
  disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate generate the
  schema from the JPA entity mappings (`ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` keeps
  `@DataJpaTest` from swapping in its own default embedded datasource, so the explicit
  H2 profile config above is the one actually used.

`WantToListen.userId`, `songId` and `albumId` are plain `Long` columns with no
`@ManyToOne` associations, so the tests use plausible literal `Long` ids directly
instead of persisting real `User`/`Song`/`Album` rows first.

`WantToListen.id` is an `Integer`, while `WantToListenRepository` is currently declared
as `CrudRepository<WantToListen, Long>` (see the "Known issue" in the
[`WantToListen` repository docs](../../features/WantToListen/wantToListenRepositoryDocumentation.md)).
Because of this mismatch, tests cannot pass the `Integer` returned by
`WantToListen.getId()` straight into `findById`/`deleteById` — the code would not
compile. Instead, every lookup in this test converts the id with `.longValue()`, e.g.
`wantToListenRepository.findById(persisted.getId().longValue())`.

### What each test verifies

- **`savesWantToListenAndPopulatesGeneratedIdAndCreatedAt`** — saving a new
  `WantToListen` through `WantToListenRepository.save` generates an id and populates
  `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — an entry persisted directly via
  `TestEntityManager` can be read back through
  `wantToListenRepository.findById(...longValue())`, with `userId`, `songId`,
  `albumId`, and `createdAt` all round-tripping correctly.
- **`findAllReturnsAllPersistedWantToListenEntries`** —
  `wantToListenRepository.findAll()` returns every persisted entry.
- **`deleteByIdRemovesWantToListenEntry`** — after
  `wantToListenRepository.deleteById(...longValue())`, the entry can no longer be found
  by `findById`.
- **`supportsWantToListenEntryForSongWithoutAlbum`** — a `WantToListen` entry can be
  persisted with `songId` set and `albumId = null` (saving a song rather than an
  album); both fields round-trip correctly, confirming `songId`/`albumId` are
  independently nullable at the JPA level as documented.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`. They also do not exercise the `WantToListenRepository` id-type mismatch
described above beyond working around it — see the
[repository docs](../../features/WantToListen/wantToListenRepositoryDocumentation.md)
for the suggested fix (`CrudRepository<WantToListen, Integer>`). Nothing at the entity
or database level enforces that exactly one of `songId`/`albumId` is set — that
invariant is not exercised or enforced by these tests, only that both combinations
persist without error.

### Running the tests

```
./mvnw -Dtest=WantToListenRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
