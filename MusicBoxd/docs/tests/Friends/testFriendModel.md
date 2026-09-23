## Friend Repository Tests

`FriendRepositoryTest`
(`src/test/java/com/example/MusicBoxd/Repository/FriendRepositoryTest.java`) is a
`@DataJpaTest` covering [`FriendRepository`](../../features/Friends/friendRepositoryDocumentation.md)
and, by extension, the [`Friend`](../../features/Friends/friendModelDocumentation.md)
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

`Friend.requesterId` and `Friend.receiverId` are plain `Long` columns with no
`@ManyToOne` association to `User`, so the tests use plausible literal `Long` ids
directly instead of persisting real `User` rows first.

### What each test verifies

- **`savesFriendAndPopulatesGeneratedIdAndCreatedAt`** — saving a new `Friend` through
  `FriendRepository.save` generates an id and populates `createdAt`.
- **`findByIdReturnsAllPersistedFields`** — a friend link persisted directly via
  `TestEntityManager` can be read back through `friendRepository.findById`, with
  `requesterId`, `receiverId`, `status`, and `createdAt` all round-tripping correctly.
- **`findAllReturnsAllPersistedFriends`** — `friendRepository.findAll()` returns every
  persisted friend link (verified by `status`, in any order).
- **`deleteByIdRemovesFriend`** — after `friendRepository.deleteById`, the friend link
  can no longer be found by `findById`.
- **`persistsFriendStatusTransitionToAccepted`** — `status` is a plain, mutable
  `String` column, so a `Friend` initially persisted with `status = "PENDING"` can have
  its status updated (e.g. to `"ACCEPTED"`) and that change is correctly persisted.
- **`findByRequesterIdAndReceiverIdReturnsMatchingFriendRequest`** /
  **`findByRequesterIdAndReceiverIdReturnsEmptyWhenNoMatch`** — looks up a friend link
  in the requester → receiver direction, and returns `Optional.empty()` when there is
  none.
- **`findByReceiverIdAndRequesterIdReturnsMatchingFriendRequest`** — the mirrored
  lookup, in the receiver → requester direction.
- **`findByReceiverIdAndStatusReturnsOnlyMatchingFriendships`** /
  **`findByRequesterIdAndStatusReturnsOnlyMatchingFriendships`** — each method returns
  only the friend links for the given receiver/requester that also match the given
  status, excluding links with a different status or for a different user.
- **`findByStatusReturnsAllFriendshipsWithThatStatus`** /
  **`findByStatusReturnsEmptyListWhenNoneMatch`** — `findByStatus` returns every friend
  link with a given status regardless of requester/receiver, and an empty list when
  none match.
- **`findByRequesterIdAndStatusOrReceiverIdAndStatusReturnsFriendshipsOnEitherSide`** —
  called with the same user id/status on both sides, this returns friend links where
  that user is either the requester or the receiver with that status, letting callers
  find "my accepted friends" regardless of who sent the original request.
- **`countByReceiverIdAndStatusCountsOnlyMatchingFriendships`** /
  **`countByRequesterIdAndStatusCountsOnlyMatchingFriendships`** — each count only
  includes friend links for the given user/status combination.
- **`existsByRequesterAndReceiverAndStatusMatchesOnlyTheGivenDirectionAndStatus`** /
  **`existsByReceiverAndRequesterAndStatusMatchesOnlyTheGivenDirectionAndStatus`** —
  each `existsBy...AndStatus` query returns `true` only for a friend link with the given
  direction and `Friend.Status`, and `false` for a different status or the reverse
  direction. (These methods previously threw `InvalidDataAccessApiUsageException`
  because the enum was bound against the `String` column. The queries now compare
  against the enum's name.)

### Known limitation

These tests run against a Hibernate-generated H2 schema, not the real Flyway-migrated
Postgres schema, so they cannot verify database-level constraints defined in
`V1__init.sql`. They also cannot verify referential integrity between `Friend` and
`User`, since none is enforced at the entity level (see the
[`Friend` model docs](../../features/Friends/friendModelDocumentation.md)).

### Running the tests

```
./mvnw -Dtest=FriendRepositoryTest test
```

or, as part of the full suite:

```
./mvnw test
```
