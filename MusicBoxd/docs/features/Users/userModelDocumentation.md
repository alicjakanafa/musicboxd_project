## User Model

`User` (`com.example.MusicBoxd.Model.User`) represents a registered MusicBoxd account —
the person who signs in (via Google), builds a profile, and later writes reviews and
comments on albums and songs. It maps to the `users` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field                | Column                 | Type            | Notes                                                              |
|-----------------------|------------------------|-----------------|---------------------------------------------------------------------|
| `id`                  | `id`                   | `Long`          | Primary key, auto-generated (`bigserial` / `IDENTITY`).             |
| `googleUserId`        | `google_user_id`       | `String`        | External id from Google sign-in. Optional (nullable) at the DB level. |
| `username`            | `username`              | `String`        | Display/handle name shown throughout the app.                       |
| `email`               | `email`                 | `String`        | Contact email. Optional.                                            |
| `bio`                 | `bio`                   | `String`        | Free-text profile bio. Optional.                                    |
| `profilePictureUrl`   | `profile_picture_url`   | `String`        | URL to the user's avatar image. Optional.                           |
| `createdAt`           | `created_at`            | `LocalDateTime` | When the account was created.                                       |

### Creating a user

Besides the no-args constructor (used by JPA/Hibernate), `User` has a constructor that
takes the user-facing fields and automatically stamps `createdAt` to "now":

```java
User user = new User(
    googleUserId,     // may be null
    username,
    email,             // may be null
    bio,               // may be null
    profilePictureUrl  // may be null
);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time (it is not a database default applied on
save; see [[testUserModel]] for how this is verified).

### Constraints and known limitations

The `V1__init.sql` migration declares two constraints on the real (Postgres) `users`
table that the `User` entity does **not** currently enforce at the JPA level:

- `username` is `NOT NULL UNIQUE`
- `google_user_id` is `UNIQUE`

Neither field is annotated with `@Column(nullable = false)` / `@Column(unique = true)`
on the entity, so Hibernate will not reject a `null` or duplicate `username`/
`googleUserId` when persisting through JPA outside of what the real Postgres schema
would reject. This is a schema/entity drift, not a bug that blocks normal use — see the
"Known limitation" note in the repository test documentation
([[testUserModel]]) for why the current test suite can't catch it either.

### Related documentation

- [User Repository](../Users/userRepositoryDocumentation.md)
- [User repository tests](../../tests/Users/testUserModel.md)
