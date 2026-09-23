## User Favourite Artist Model

`UserFavouriteArtist` (`com.example.MusicBoxd.Model.UserFavouriteArtist`) represents
one of a [`User`](../Users/userModelDocumentation.md)'s favourite
[`Artist`](../Artists/artistModelDocumentation.md)s. It maps to the
`USER_FAVOURITE_ARTISTS` table, created in
`src/main/resources/db/migration/V24__create_user_favourite_artists.sql`.

### Fields

| Field      | Column      | Type   | Notes                                                               |
|------------|-------------|--------|-----------------------------------------------------------------------|
| `id`       | `id`        | `Long` | Primary key, auto-generated (`IDENTITY`).                             |
| `userId`   | `user_id`   | `Long` | Id of the [`User`](../Users/userModelDocumentation.md) the favourite belongs to. `NOT NULL`. |
| `artistId` | `artist_id` | `Long` | Id of the favourited [`Artist`](../Artists/artistModelDocumentation.md). `NOT NULL`. |

Unlike `UserFavouriteAlbum`, this entity has no `position` field — favourite artists
are not ranked/ordered, just a set of "liked" artists per user.

`userId` and `artistId` are plain `Long` columns — there is no `@ManyToOne` association
to `User` or `Artist` on this entity, so JPA does not enforce referential integrity at
the entity level, even though the underlying migration declares foreign keys
(`fk_user_favourite_artist_user`, `fk_user_favourite_artist_artist`, both
`ON DELETE CASCADE`) and a `unique_user_favourite_artist` constraint on
`(user_id, artist_id)` so a user cannot favourite the same artist twice.

That unique constraint is declared at the database (Flyway/Postgres) level only; it is
not expressed on the entity itself, so it is not enforced by the Hibernate-generated H2
schema used in `@DataJpaTest`s (see the
[repository tests](../../tests/Favourites/testUserFavouriteArtistModel.md) for
details).

### Creating a favourite artist entry

Besides the no-args constructor (used by JPA/Hibernate), `UserFavouriteArtist` has a
constructor that takes both user-facing fields:

```java
UserFavouriteArtist favourite = new UserFavouriteArtist(
    userId,
    artistId
);
```

### Related documentation

- [User Favourite Artist Repository](./userFavouriteArtistRepositoryDocumentation.md)
- [User Favourite Artist repository tests](../../tests/Favourites/testUserFavouriteArtistModel.md)
