## Want To Listen Model

`WantToListen` (`com.example.MusicBoxd.Model.WantToListen`) represents a
[`User`](../Users/userModelDocumentation.md) saving an album or song to a "want to
listen" list for later. It maps to the `want_to_listen` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field       | Column       | Type            | Notes                                                                |
|-------------|--------------|-----------------|----------------------------------------------------------------------|
| `id`        | `id`         | `Integer`        | Primary key, auto-generated (`serial` / `IDENTITY`). Note this entity uses `Integer`, not `Long`, unlike most other entities in the project. |
| `userId`    | `user_id`    | `Long`           | Id of the `User` who saved this item.                                  |
| `songId`    | `song_id`    | `Long`           | Id of the [`Song`](../Songs/songModelDocumentation.md) saved, or `null` when the saved item is an album. |
| `albumId`   | `album_id`   | `Long`           | Id of the [`Album`](../Albums/albumModelDocumentation.md) saved, or `null` when the saved item is a song. |
| `createdAt` | `created_at` | `LocalDateTime`  | When the item was added to the user's want-to-listen list.             |

`userId`, `songId` and `albumId` are plain `Long` columns — there are no `@ManyToOne`
associations on this entity, so JPA does not enforce referential integrity with `User`,
`Song` or `Album` at the entity level. Nothing at the entity or database level enforces
that exactly one of `songId`/`albumId` is set.

`WantToListen` is annotated with Lombok's `@Data` in addition to `@Getter`/`@Setter`,
which also generates `equals`/`hashCode`/`toString` based on all fields.

### Creating a want-to-listen entry

Besides the no-args constructor (used by JPA/Hibernate), `WantToListen` has a
constructor that takes the user-facing fields and automatically stamps `createdAt` to
"now":

```java
WantToListen entry = new WantToListen(
    userId,
    songId,    // may be null if this entry is an album
    albumId    // may be null if this entry is a song
);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time.

### Related documentation

- [Want To Listen Repository](./wantToListenRepositoryDocumentation.md)
- [Want To Listen repository tests](../../tests/WantToListen/testWantToListenModel.md)
