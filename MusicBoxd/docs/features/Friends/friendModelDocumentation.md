## Friend Model

`Friend` (`com.example.MusicBoxd.Model.Friend`) represents a friendship link (or friend
request) between two [`User`](../Users/userModelDocumentation.md) accounts — one user
(the requester) asking to connect with another (the receiver), and the status of that
connection. It maps to the `friends` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field         | Column         | Type            | Notes                                                              |
|---------------|----------------|-----------------|----------------------------------------------------------------------|
| `id`          | `id`           | `Long`          | Primary key, auto-generated (`serial` / `IDENTITY`).                 |
| `requesterId` | `requester_id` | `Long`          | Id of the [`User`](../Users/userModelDocumentation.md) who sent the friend request. |
| `receiverId`  | `receiver_id`  | `Long`          | Id of the `User` who received the friend request.                    |
| `status`      | `status`       | `String`        | Free-text status of the friendship (e.g. pending/accepted); not constrained to an enum at the entity or DB level. |
| `createdAt`   | `created_at`   | `LocalDateTime` | When the friend request/link was created.                            |

`requesterId` and `receiverId` are plain `Long` columns — there is no `@ManyToOne`
association to `User` on this entity, so JPA does not enforce referential integrity
between `Friend` and `User` at the entity level.

### Creating a friend request

Besides the no-args constructor (used by JPA/Hibernate), `Friend` has a constructor that
takes the user-facing fields and automatically stamps `createdAt` to "now":

```java
Friend friend = new Friend(
    requesterId,
    receiverId,
    status   // e.g. "pending"
);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time (it is not a database default applied on
save).

### Related documentation

- [Friend Repository](./friendRepositoryDocumentation.md)
- [Friend repository tests](../../tests/Friends/testFriendModel.md)
