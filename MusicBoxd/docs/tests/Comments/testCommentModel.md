## Comment Repository Tests

`CommentRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/CommentRepositoryTest.java`)
is a `@DataJpaTest` covering `CommentRepository` and, by extension, the
`Comment` entity's persistence behavior.

### Test setup

The class runs against an in-memory H2 database instead of the project's
normal PostgreSQL/Flyway-managed database, so it stays fast and does not
require a running Postgres instance. This is opt-in and isolated from the
rest of the suite:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at H2,
  disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate generate
  the schema from the JPA entity mappings (`ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)`
  keeps `@DataJpaTest` from swapping in its own default embedded datasource,
  so the explicit H2 profile config above is the one actually used.

Because a `Comment` references a `User` and a `Review` by id, each test first
persists a `User`, an `Album`, and a `Review` via `TestEntityManager` before
exercising `CommentRepository`, so the foreign-key-style ids are real,
persisted rows.

### What each test verifies

- **`savesCommentLinkedToUserAndReview`** — saving a new `Comment` through
  `CommentRepository.save` generates an id and preserves `userId`, `reviewId`,
  and the auto-set `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a comment persisted directly via
  `TestEntityManager` can be read back through `commentRepository.findById`,
  with `userId`, `reviewId`, `content`, and `createdAt` all round-tripping
  correctly.
- **`findAllReturnsAllPersistedComments`** — `commentRepository.findAll()`
  returns every persisted comment (verified by comment content, in any
  order).
- **`deleteByIdRemovesComment`** — after `commentRepository.deleteById`, the
  comment can no longer be found by `findById`.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real
Flyway-migrated Postgres schema, so they cannot verify database-level
constraints defined in `V7__create_comments_table.sql` (none are declared for
`comments` beyond the primary key, so this mainly matters for related tables
like `users`, which does declare `NOT NULL`/`UNIQUE` constraints not mirrored
in the JPA entity).

### Running the tests

```
./mvnw -Dtest=CommentRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
