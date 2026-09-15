## Tag Repository Tests

`TagRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/TagRepositoryTest.java`) is
a `@DataJpaTest` covering `TagRepository` and, by extension, the `Tag`
entity's persistence behavior.

### Test setup

The test class runs against an in-memory H2 database instead of the
project's normal PostgreSQL/Flyway-managed datasource:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at
  H2, disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate
  generate the schema from the entity mappings
  (`spring.jpa.hibernate.ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  stops Spring Boot's test autoconfiguration from swapping in its own
  default embedded database, so the H2 datasource configured above is the
  one actually used.

This keeps the suite fast and independent of a running Postgres instance. It
does not affect `MusicBoxdApplicationTests` or any other test, since the
`datajpatest` profile is only opted into explicitly by this class.

### What each test verifies

- **`savesTagAndGeneratesId`** — saving a new `Tag("Shoegaze")` populates a
  non-null `id` and preserves the `name`.
- **`findByIdReturnsPersistedTag`** — a `Tag` persisted directly via
  `TestEntityManager` can be found through `TagRepository.findById` with the
  correct `name`.
- **`findAllReturnsAllPersistedTags`** — persisting two tags (`"Rock"`,
  `"Pop"`) and calling `findAll()` returns both, matched by name regardless
  of order.
- **`deleteByIdRemovesTag`** — deleting a persisted tag by id makes it
  unfindable afterward.

### Running the tests

```
./mvnw -Dtest=TagRepositoryTest test
```

or as part of the full suite:

```
./mvnw test
```

### Known limitation

The `tags` table's `name` column has no `NOT NULL`/`UNIQUE` constraint, and
because this test slice uses Hibernate-generated (not Flyway-migrated)
schema, these tests do not (and currently cannot) verify any such
database-level constraint even if one were added later — see
`docs/features/repository-tests.md` for the project-wide note on this.
