## Song Repository Tests

`src/test/java/com/example/MusicBoxd/Repository/SongRepositoryTest.java` is a
`@DataJpaTest` test class covering `Song` persistence through
`SongRepository`.

### Test setup

- `@DataJpaTest` + `@ActiveProfiles("datajpatest")` +
  `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`.
- The `datajpatest` Spring profile (`src/test/resources/application-datajpatest.properties`)
  points at an in-memory H2 database, disables Flyway
  (`spring.flyway.enabled=false`), and lets Hibernate generate the schema
  from the entity mappings (`spring.jpa.hibernate.ddl-auto=create-drop`).
  This keeps the test fast and independent of a running local Postgres
  instance — the profile is opt-in, so it has no effect on
  `MusicBoxdApplicationTests` or other tests that use the real
  Postgres/Flyway-backed `application-test.properties` configuration.
- `TestEntityManager` is used to set up a related `Album` row before each
  test that needs one, since `Song.albumId` is a plain column rather than a
  managed JPA relation.

### What each test verifies

- **`savesSongLinkedToPersistedAlbum`** — persists an `Album`, then saves a
  `Song` via `songRepository.save(...)` referencing that album's id.
  Verifies the saved `Song` gets a generated `id`, that `albumId` matches the
  persisted album, and that `createdAt` was populated.
- **`findByIdReturnsAllPersistedFields`** — persists an `Album` and a `Song`
  directly via `TestEntityManager`, then reads it back with
  `songRepository.findById(...)`. Verifies every field (`externalId`,
  `albumId`, `title`, `trackNumber`, `songUrl`, `songImageUrl`, `createdAt`)
  round-trips correctly.
- **`findAllReturnsAllSongsRegardlessOfAlbum`** — persists two songs under the
  same album and asserts `songRepository.findAll()` returns both, matched by
  title, regardless of album grouping.
- **`deleteByIdRemovesSong`** — persists a song, deletes it via
  `songRepository.deleteById(...)`, and asserts a subsequent `findById`
  returns empty.

### Known limitation

The `songs` table in the Flyway migration does not declare `NOT NULL`
constraints, and neither does the `Song` entity, so these tests do not (and
cannot, given the H2/Hibernate-generated schema) verify database-level
constraint enforcement — only that basic CRUD round-trips behave correctly.

### Running the tests

```
./mvnw -Dtest=SongRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
