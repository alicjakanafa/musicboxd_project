## Tag Model

`Tag` (`com.example.MusicBoxd.Model.Tag`) is a JPA entity mapped to the `TAGS`
table. It represents a single, reusable label (e.g. a genre or mood word like
"Shoegaze" or "Jazz Fusion") that can be attached elsewhere in the product to
categorize content.

### Why / when it's used

Tags exist so that content (albums, songs, etc.) can be organized and
filtered by free-form labels rather than a fixed genre list. The entity
itself only defines the label; nothing in the current codebase yet links a
`Tag` to another entity (no `@ManyToOne`/`@ManyToMany` mapping exists on
`Tag` or elsewhere), so today it functions as a standalone lookup/catalog
row.

### Fields

| Field  | Column | Type   | Notes |
|--------|--------|--------|-------|
| `id`   | `id`   | `Long` | Primary key, auto-generated (`GenerationType.IDENTITY`, backed by `bigserial` in Postgres). |
| `name` | `name` | `String` | The tag's display label, e.g. `"Shoegaze"`. |

This matches the `tags` table exactly as defined in
`src/main/resources/db/migration/V3__create_tags_table.sql`:

```sql
CREATE TABLE tags (
     id bigserial PRIMARY KEY,
     name VARCHAR(255)
);
```

### Limitations

- `name` has no `NOT NULL` or `UNIQUE` constraint in the migration, and the
  entity does not add one via `@Column`, so duplicate or `null` tag names are
  currently possible at the database level.
- There is no relationship mapping from `Tag` to any other entity yet — it is
  a plain, unattached row.

### Example

```java
Tag tag = new Tag("Shoegaze");
Tag saved = tagRepository.save(tag);
// saved.getId() is now populated by the database
```
