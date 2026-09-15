## Message Repository Tests

`MessageRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/MessageRepositoryTest.java`) is a
`@DataJpaTest` covering [`MessageRepository`](../../features/Messages/messageRepositoryDocumentation.md)
and, by extension, the [`Message`](../../features/Messages/messageModelDocumentation.md)
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

`Message.senderId` and `receiverId` are plain `Long` columns with no `@ManyToOne`
association to `User`, so the tests use plausible literal `Long` ids directly instead
of persisting real `User` rows first.

### What each test verifies

- **`savesMessageAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `Message`
  through `MessageRepository.save` generates an id and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a message persisted directly via
  `TestEntityManager` can be read back through `messageRepository.findById`, with
  `senderId`, `receiverId`, `content`, `songTitle`, `songArtist`, `songImageUrl`,
  `songPreviewUrl`, and `createdAt` all round-tripping correctly.
- **`findAllReturnsAllPersistedMessages`** — `messageRepository.findAll()` returns
  every persisted message (verified by `content`, in any order).
- **`deleteByIdRemovesMessage`** — after `messageRepository.deleteById`, the message
  can no longer be found by `findById`.
- **`newMessageDefaultsToUnread`** — a `Message` created through its constructor and
  persisted has `read = false` by default, confirming the constructor's default is
  actually preserved through a save (a caller must explicitly call `setRead(true)` to
  mark it read).

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`. They also do not cover the song-share fields being copied verbatim
rather than linked to a real `Song` row (see the
[`Message` model docs](../../features/Messages/messageModelDocumentation.md)).

### Running the tests

```
./mvnw -Dtest=MessageRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
