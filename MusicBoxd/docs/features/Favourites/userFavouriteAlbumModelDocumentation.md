## User Favourite Album Model

`UserFavouriteAlbum` (`com.example.MusicBoxd.Model.UserFavouriteAlbum`) represents one
of a [`User`](../Users/userModelDocumentation.md)'s favourite albums, at a specific
ranked position (a "top four"/favourites list). It maps to the `top_four` table.

That table was originally created as `user_favourite_albums` in
`src/main/resources/db/migration/V20__create_favourite_albums.sql` and renamed to
`top_four` in `src/main/resources/db/migration/V22__rename_user_favourite_albums.sql`.

### Fields

| Field      | Column      | Type      | Notes                                                                 |
|------------|-------------|-----------|------------------------------------------------------------------------|
| `id`       | `id`        | `Long`    | Primary key, auto-generated (`IDENTITY`).                              |
| `userId`   | `user_id`   | `Long`    | Id of the [`User`](../Users/userModelDocumentation.md) the favourite belongs to. |
| `albumId`  | `album_id`  | `Long`    | Id of the favourited [`Album`](../Albums/albumModelDocumentation.md).  |
| `position` | `position`  | `Integer` | The album's rank/slot within the user's favourites (e.g. 0-3 for a "top four"). |

`userId` and `albumId` are plain `Long` columns — there is no `@ManyToOne` association
to `User` or `Album` on this entity, so JPA does not enforce referential integrity at
the entity level, even though the underlying migration declares foreign keys
(`fk_favourite_user`, `fk_favourite_album`) and two unique constraints:

- `unique_user_album` — a user can only favourite the same album once.
- `unique_user_position` — a user can only have one album in a given position.

Both unique constraints are declared at the database (Flyway/Postgres) level only; they
are not expressed on the entity itself (no `@Table(uniqueConstraints = ...)`), so they
are not enforced by the Hibernate-generated H2 schema used in `@DataJpaTest`s (see the
[repository tests](../../tests/Favourites/testUserFavouriteAlbumModel.md) for details).

### Creating a favourite album entry

Besides the no-args constructor (used by JPA/Hibernate), `UserFavouriteAlbum` has a
constructor that takes all three user-facing fields:

```java
UserFavouriteAlbum favourite = new UserFavouriteAlbum(
    userId,
    albumId,
    position   // e.g. 0 for the first favourite slot
);
```

### Related documentation

- [User Favourite Album Repository](./userFavouriteAlbumRepositoryDocumentation.md)
- [User Favourite Album repository tests](../../tests/Favourites/testUserFavouriteAlbumModel.md)
