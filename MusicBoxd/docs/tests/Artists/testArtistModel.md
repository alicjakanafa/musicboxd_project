## Artist Repository Tests

`ArtistRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/ArtistRepositoryTest.java`)
is a `@DataJpaTest` that verifies `Artist` can be persisted and retrieved
through `ArtistRepository`.

### Test setup

The application's normal datasource points at a local PostgreSQL database
managed by Flyway. Requiring a live Postgres instance for a repository test
would make it slow and environment-dependent, so this test instead runs
against an in-memory H2 database via an opt-in Spring profile:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at
  H2, disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate
  generate the schema from the JPA entity mappings
  (`spring.jpa.hibernate.ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  tells `@DataJpaTest` to use that configured H2 datasource instead of trying
  to auto-detect/replace it.

Because the profile is opt-in, this has no effect on
`MusicBoxdApplicationTests` or any other test using the default Postgres
configuration.

### What each test verifies

- **`savesArtistAndGeneratesId`** — saving a new `Artist` via
  `artistRepository.save(...)` populates a generated `id` and preserves the
  `name`.
- **`findByIdReturnsPersistedArtist`** — an artist persisted directly through
  `TestEntityManager` can be found by id via the repository, with the
  correct `name`.
- **`findAllReturnsAllPersistedArtists`** — persisting two artists and
  calling `findAll()` returns both, matched by name regardless of order.
- **`deleteByIdRemovesArtist`** — after `deleteById(...)`, `findById(...)`
  for that id returns empty.

### Known limitation

The `name` column has no `NOT NULL` constraint enforced at the entity level,
and this test suite (running against an H2 schema generated from the entity
mappings) does not verify any Postgres-level constraint, since the
`artists` migration doesn't declare one either — there is nothing to test
here beyond basic persistence.

### Running the tests

```
./mvnw -Dtest=ArtistRepositoryTest test
```

or as part of the full suite:

```
./mvnw test
```
