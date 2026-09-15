## List Item Model

`ListItem` (`com.example.MusicBoxd.Model.ListItem`) represents a single entry within a
[`List`](../Lists/listModelDocumentation.md) — either an album or a song, at a given
position in that list. It maps to the `list_items` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field       | Column       | Type            | Notes                                                                |
|-------------|--------------|-----------------|--------------------------------------------------------------------|
| `id`        | `id`         | `Integer`        | Primary key, auto-generated (`serial` / `IDENTITY`). Note this entity uses `Integer`, not `Long`, unlike most other entities in the project. |
| `listId`    | `list_id`    | `Long`           | Id of the owning [`List`](../Lists/listModelDocumentation.md).       |
| `albumId`   | `album_id`   | `Long`           | Id of the [`Album`](../Albums/albumModelDocumentation.md) this entry refers to, or `null` when the entry is a song. |
| `songId`    | `song_id`    | `Long`           | Id of the [`Song`](../Songs/songModelDocumentation.md) this entry refers to, or `null` when the entry is an album. |
| `position`  | `position`   | `Integer`        | Ordering position of this item within its list.                      |
| `createdAt` | `created_at` | `LocalDateTime`  | When the item was added to the list.                                 |

`listId`, `albumId` and `songId` are plain `Long` columns — there are no `@ManyToOne`
associations on this entity, so JPA does not enforce referential integrity with `List`,
`Album` or `Song` at the entity level. Nothing at the entity or database level enforces
that exactly one of `albumId`/`songId` is set.

`ListItem` is annotated with Lombok's `@Data` in addition to `@Getter`/`@Setter`, which
also generates `equals`/`hashCode`/`toString` based on all fields (the other simpler
entities like `Friend`/`Like`/`List` only use `@Getter`/`@Setter`).

### Creating a list item

Besides the no-args constructor (used by JPA/Hibernate), `ListItem` has a constructor
that takes the user-facing fields and automatically stamps `createdAt` to "now":

```java
ListItem item = new ListItem(
    listId,
    albumId,   // may be null if this entry is a song
    songId,    // may be null if this entry is an album
    position
);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time.

### Related documentation

- [List Item Repository](./listItemRepositoryDocumentation.md)
- [List model](../Lists/listModelDocumentation.md)
- [List Item repository tests](../../tests/ListItems/testListItemModel.md)
