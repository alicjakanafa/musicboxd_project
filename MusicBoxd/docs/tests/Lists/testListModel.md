## List Repository Tests

`ListRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/ListRepositoryTest.java`) is a
`@DataJpaTest` covering [`ListRepository`](../../features/Lists/listRepositoryDocumentation.md)
and, by extension, the [`List`](../../features/Lists/listModelDocumentation.md)
entity's persistence behavior.

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

Because `List` (`com.example.MusicBoxd.Model.List`) shares a simple name with
`java.util.List`, the test fully qualifies every reference to the entity as
`com.example.MusicBoxd.Model.List` and only imports `java.util.List` for the
collection type returned by `findAll()`, avoiding the ambiguity noted in the
[`List` model docs](../../features/Lists/listModelDocumentation.md).

`List.userId` is a plain `Long` column with no `@ManyToOne` association to `User`, so
the tests use plausible literal `Long` ids directly instead of persisting a real `User`
row first.

### What each test verifies

- **`savesListAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `List` through
  `ListRepository.save` generates an id and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a list persisted directly via
  `TestEntityManager` can be read back through `listRepository.findById`, with
  `userId`, `title`, `description`, and `createdAt` all round-tripping correctly.
- **`findAllReturnsAllPersistedLists`** — `listRepository.findAll()` returns every
  persisted list (verified by `title`, in any order).
- **`deleteByIdRemovesList`** — after `listRepository.deleteById`, the list can no
  longer be found by `findById`.
- **`savesListWithNullDescription`** — a `List` can be saved with `description = null`,
  confirming `description` is optional at the JPA level.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`, such as the `VARCHAR(50)` limit on `title` (see the
[`List` model docs](../../features/Lists/listModelDocumentation.md)).

### Running the tests

```
./mvnw -Dtest=ListRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
