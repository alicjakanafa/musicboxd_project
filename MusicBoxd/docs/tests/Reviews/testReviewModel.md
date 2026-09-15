## Review Repository Tests

`ReviewRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/ReviewRepositoryTest.java`) is a
`@DataJpaTest` suite covering [[reviewRepositoryDocumentation|ReviewRepository]] and, by
extension, the [[reviewModelDocumentation|Review]] entity's persistence behavior.

### Test setup / strategy

The suite uses an in-memory H2 database instead of the project's normal PostgreSQL/Flyway
setup, so the tests are fast and don't require a running database:

- `@DataJpaTest` + `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at an H2 in-memory
  datasource, disables Flyway (`spring.flyway.enabled=false`), and lets Hibernate generate
  the schema from the entity mappings (`spring.jpa.hibernate.ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` tells Spring
  Boot to use that configured H2 datasource as-is rather than substituting its own embedded
  database.
- This profile is opt-in and only affects classes that declare it, so it has no effect on
  `MusicBoxdApplicationTests` or anything else running against the real Postgres/Flyway
  configuration.
- `TestEntityManager` is used to arrange related `User`, `Album`, and `Song` rows before
  exercising `ReviewRepository`, since `Review` references them by plain id columns rather
  than JPA relationships.

### What each test verifies

- **`savesReviewLinkedToUserAlbumAndSong`** — persists a `User`, `Album`, and `Song`, then
  saves a `Review` referencing all three. Asserts the saved review gets a generated `id`,
  that `userId`/`albumId`/`songId` round-trip correctly, and that `createdAt` is populated.
- **`findByIdReturnsAllPersistedFieldsIncludingRating`** — persists a `User` and `Album`,
  then a `Review` with `songId = null` (album-level review). Reads it back via
  `findById` and asserts every field round-trips, including comparing `rating` with
  `isEqualByComparingTo` (rather than `equals`) to avoid `BigDecimal` scale mismatches, and
  confirms `songId` stays `null`.
- **`findAllReturnsAllPersistedReviews`** — persists two reviews for the same user/album and
  asserts `findAll()` returns both, matched by `header` in any order.
- **`deleteByIdRemovesReview`** — persists a review, deletes it by id, and asserts
  `findById` afterward returns empty.

### Running these tests

```
./mvnw -Dtest=ReviewRepositoryTest test
```

Or as part of the full suite:

```
./mvnw test
```

### Known limitation

The `reviews` table has no `NOT NULL`/foreign-key constraints beyond the primary key, and
the `Review` entity declares no Bean Validation or JPA relationship annotations either, so
these tests only verify successful round-trip persistence — they don't (and currently can't,
against an H2-generated schema) verify referential integrity against `users`/`albums`/`songs`.
