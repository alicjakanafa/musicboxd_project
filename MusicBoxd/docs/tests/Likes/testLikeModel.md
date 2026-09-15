## Like Repository Tests

`LikeRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/LikeRepositoryTest.java`) is a
`@DataJpaTest` covering [`LikeRepository`](../../features/Likes/likeRepositoryDocumentation.md)
and, by extension, the [`Like`](../../features/Likes/likeModelDocumentation.md)
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

`Like.userId`, `reviewId` and `commentId` are plain `Long` columns with no
`@ManyToOne` associations, so the tests use plausible literal `Long` ids directly
instead of persisting real `User`/`Review`/`Comment` rows first.

### What each test verifies

- **`savesLikeAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `Like` through
  `LikeRepository.save` generates an id and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a like persisted directly via
  `TestEntityManager` can be read back through `likeRepository.findById`, with
  `userId`, `reviewId`, `commentId`, and `createdAt` all round-tripping correctly.
- **`findAllReturnsAllPersistedLikes`** — `likeRepository.findAll()` returns every
  persisted like.
- **`deleteByIdRemovesLike`** — after `likeRepository.deleteById`, the like can no
  longer be found by `findById`.
- **`supportsLikeOnCommentWithoutReview`** — a `Like` can be persisted with
  `reviewId = null` and `commentId` set (liking a comment rather than a review); both
  fields round-trip correctly, confirming `reviewId`/`commentId` are independently
  nullable at the JPA level as documented.

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`. Nothing at the entity or database level enforces that exactly one of
`reviewId`/`commentId` is set — that invariant is not exercised or enforced by these
tests, only that both combinations persist without error (see the
[`Like` model docs](../../features/Likes/likeModelDocumentation.md)).

### Running the tests

```
./mvnw -Dtest=LikeRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
