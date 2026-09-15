## User Repository Tests

`UserRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/UserRepositoryTest.java`) is a
`@DataJpaTest` suite that verifies the [`User` model](../../features/Users/userModelDocumentation.md)
and [`UserRepository`](../../features/Users/userRepositoryDocumentation.md) can actually
persist, read back, list and delete data correctly.

### Test setup

The test uses an in-memory H2 database instead of the project's normal PostgreSQL/Flyway
setup, so it runs quickly and without any external database running:

- `@ActiveProfiles("datajpatest")` activates
  `src/test/resources/application-datajpatest.properties`, which points at H2, disables
  Flyway (`spring.flyway.enabled=false`), and lets Hibernate create the schema directly
  from the JPA entity mappings (`spring.jpa.hibernate.ddl-auto=create-drop`).
- `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` tells
  Spring Boot not to swap in its own auto-detected embedded database, so the
  `datajpatest` profile's H2 datasource is the one actually used.
- This profile is opt-in per test class, so it has no effect on
  `MusicBoxdApplicationTests` or any other test — those keep using the real Postgres
  `MusicBoxd_Test` database via Flyway, unchanged.

### What each test verifies

- **`savesUserAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `User` via
  `userRepository.save(...)` assigns a generated `id` and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — after persisting a user (via
  `TestEntityManager.persistFlushFind`), `userRepository.findById(...)` returns a user
  whose `googleUserId`, `username`, `email`, `bio`, `profilePictureUrl` and `createdAt`
  all match what was saved.
- **`savesUserWithOnlyRequiredFieldsPopulated`** — a user can be saved with only
  `username` set and every other field `null` (`googleUserId`, `email`, `bio`,
  `profilePictureUrl`), confirming those fields are treated as optional at the JPA
  level.
- **`findAllReturnsAllPersistedUsers`** — after persisting two users,
  `userRepository.findAll()` returns both, matched by `username`.
- **`deleteByIdRemovesUser`** — after persisting a user and calling
  `userRepository.deleteById(...)`, `findById` on the same id returns empty.

### Known limitation

Because this test slice lets Hibernate generate its own schema from the entity mappings
(rather than running against the real Flyway-migrated Postgres schema), it cannot verify
the `NOT NULL` / `UNIQUE` constraints that `V1__init.sql` declares on `username` and
`google_user_id` — those are only enforced by the real Postgres database, not by these
tests. See the entity-level note in
[the User model docs](../../features/Users/userModelDocumentation.md#constraints-and-known-limitations).

### Running the tests

```
./mvnw -Dtest=UserRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
