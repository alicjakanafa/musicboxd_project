## Artist Model

The `Artist` entity (`com.example.MusicBoxd.Model.Artist`) represents a music
artist or band in MusicBoxd. Albums are associated with an artist, so this is
the root entity for browsing an artist's discography.

Maps to the `ARTISTS` table (created by Flyway migration
`V2__create_artists_table.sql`).

### Fields

| Field  | Column | Type   | Notes |
|--------|--------|--------|-------|
| `id`   | `id`   | `Long` | Primary key, auto-generated (`IDENTITY` strategy / Postgres `bigserial`). |
| `name` | `name` | `String` | Artist or band name. |

### Constraints

The migration defines `name` as `VARCHAR(255)` with no `NOT NULL` or
`UNIQUE` constraint, and the entity does not add one via `@Column`
annotations either — an `Artist` can currently be saved with a `null` or
duplicate `name`. There is no explicit uniqueness rule preventing two artists
from sharing the same name.

### Example

```java
Artist artist = new Artist("Radiohead");
artistRepository.save(artist);
```

### Related documentation

- [Artist Repository](../Artists/artistRepositoryDocumentation.md)
- [Artist persistence tests](../../tests/Artists/testArtistModel.md)
