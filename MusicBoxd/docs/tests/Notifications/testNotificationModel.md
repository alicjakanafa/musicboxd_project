## Notification Repository Tests

`NotificationRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/NotificationRepositoryTest.java`) is a
`@DataJpaTest` covering [`NotificationRepository`](../../features/Notifications/notificationRepositoryDocumentation.md)
and, by extension, the
[`Notification`](../../features/Notifications/notificationModelDocumentation.md)
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

`Notification.userId`, `actorId` and `relatedId` are plain `Long` columns with no
`@ManyToOne` associations, so the tests use plausible literal `Long` ids directly
instead of persisting real `User`/review/comment rows first.

### What each test verifies

- **`savesNotificationAndPopulatesGeneratedIdAndCreatedAt`** — saving a new
  `Notification` through `NotificationRepository.save` generates an id and populates
  `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a notification persisted directly via
  `TestEntityManager` can be read back through `notificationRepository.findById`, with
  `userId`, `actorId`, `relatedId`, `type`, `notificationText`, and `createdAt` all
  round-tripping correctly.
- **`findAllReturnsAllPersistedNotifications`** — `notificationRepository.findAll()`
  returns every persisted notification (verified by `type`, in any order).
- **`deleteByIdRemovesNotification`** — after `notificationRepository.deleteById`, the
  notification can no longer be found by `findById`.
- **`newNotificationDefaultsToUnread`** — a `Notification` created through its
  constructor and persisted has `isRead = false` by default, confirming the
  constructor's default is actually preserved through a save (a caller must explicitly
  call `setRead(true)` to mark it read).

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`, including the Postgres-level `DEFAULT FALSE` on `is_read` (see the
[`Notification` model docs](../../features/Notifications/notificationModelDocumentation.md)).

### Running the tests

```
./mvnw -Dtest=NotificationRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
