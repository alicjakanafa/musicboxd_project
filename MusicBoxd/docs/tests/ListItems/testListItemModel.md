## List Item Repository Tests

`ListItemRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/ListItemRepositoryTest.java`) is a
`@DataJpaTest` covering [`ListItemRepository`](../../features/ListItems/listItemRepositoryDocumentation.md)
and, by extension, the [`ListItem`](../../features/ListItems/listItemModelDocumentation.md)
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

`ListItem.listId`, `albumId` and `songId` are plain `Long` columns with no `@ManyToOne`
associations, so the tests use plausible literal `Long` ids directly instead of
persisting real `List`/`Album`/`Song` rows first.

`ListItem.id` is an `Integer`, while `ListItemRepository` is currently declared as
`CrudRepository<ListItem, Long>` (see the "Known issue" in the
[`ListItem` repository docs](../../features/ListItems/listItemRepositoryDocumentation.md)).
Because of this mismatch, tests cannot pass the `Integer` returned by
`ListItem.getId()` straight into `findById`/`deleteById` — the code would not compile.
Instead, every lookup in this test converts the id with `.longValue()`, e.g.
`listItemRepository.findById(persisted.getId().longValue())`.

### What each test verifies

- **`savesListItemAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `ListItem`
  through `ListItemRepository.save` generates an id and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a list item persisted directly via
  `TestEntityManager` can be read back through `listItemRepository.findById(...longValue())`,
  with `listId`, `albumId`, `songId`, `position`, and `createdAt` all round-tripping
  correctly.
- **`findAllReturnsAllPersistedListItems`** — `listItemRepository.findAll()` returns
  every persisted list item (verified by `position`, in any order).
- **`deleteByIdRemovesListItem`** — after `listItemRepository.deleteById(...longValue())`,
  the list item can no longer be found by `findById`.
- **`preservesOrderingPositionAcrossItemsInSameList`** — two list items saved against
  the same `listId` with different `position` values each keep their own `position`
  after persisting, confirming `position` is stored independently per row rather than
  being recalculated or shared.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`. They also do not exercise the `ListItemRepository` id-type mismatch
described above beyond working around it — see the
[repository docs](../../features/ListItems/listItemRepositoryDocumentation.md) for the
suggested fix (`CrudRepository<ListItem, Integer>`).

### Running the tests

```
./mvnw -Dtest=ListItemRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
