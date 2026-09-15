## List Model

`List` (`com.example.MusicBoxd.Model.List`) represents a user-created list (e.g. a
playlist-like collection of albums/songs), owned by a [`User`](../Users/userModelDocumentation.md).
It maps to the `lists` table, created in `src/main/resources/db/migration/V1__init.sql`.

> **Naming note:** this entity's simple name, `List`, shadows `java.util.List`. Code
> that uses both must either fully qualify one of them
> (`com.example.MusicBoxd.Model.List` / `java.util.List`) or import one and fully
> qualify the other at the point of use.

### Fields

| Field         | Column        | Type            | Notes                                                        |
|---------------|---------------|-----------------|----------------------------------------------------------------|
| `id`          | `id`          | `Long`          | Primary key, auto-generated (`serial` / `IDENTITY`).           |
| `userId`      | `user_id`     | `Long`          | Id of the `User` who owns the list.                             |
| `title`       | `title`       | `String`        | List title. The `V1__init.sql` migration limits the underlying Postgres column to `VARCHAR(50)`; the entity does not enforce this length itself. |
| `description` | `description` | `String`        | Free-text description of the list. Optional.                   |
| `createdAt`   | `created_at`  | `LocalDateTime` | When the list was created.                                      |

`userId` is a plain `Long` column — there is no `@ManyToOne` association to `User` on
this entity, so JPA does not enforce referential integrity between `List` and `User` at
the entity level.

### Creating a list

Besides the no-args constructor (used by JPA/Hibernate), `List` has a constructor that
takes the user-facing fields and automatically stamps `createdAt` to "now":

```java
com.example.MusicBoxd.Model.List list = new com.example.MusicBoxd.Model.List(
    userId,
    "Summer Favorites",
    "Songs on repeat this summer"
);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time.

### Related documentation

- [List Repository](./listRepositoryDocumentation.md)
- [List Items](../ListItems/listItemModelDocumentation.md) — the entries that belong to a list
- [List repository tests](../../tests/Lists/testListModel.md)
