## Album Model

`com.example.MusicBoxd.Model.Album` is the JPA entity representing a music
album. It maps to the `albums` table, created by the Flyway migration
`V4__create_albums_table.sql`.

### Why this exists

An album is the parent of a set of songs, and the subject of reviews and
comments. The `Album` entity is how the application stores and reads back
album metadata — title, release year, artwork — and how an album is linked
to its artist.

### Fields

| Field | Column | Type | Notes |
|---|---|---|---|
| `id` | `id` | `Long` | Primary key, auto-generated (`GenerationType.IDENTITY`). |
| `externalId` | `external_id` | `String` | Identifier from an external source (e.g. a music data provider). |
| `artistId` | `artist_id` | `Long` | Id of the owning `Artist`. Not modeled as a JPA `@ManyToOne`/`@JoinColumn` relation — it is a plain foreign-key-style column, so callers are responsible for supplying a valid artist id themselves. |
| `title` | `title` | `String` | Album title. |
| `releaseYear` | `release_year` | `Short` | Year the album was released. |
| `artworkUrl` | `artwork_url` | `String` | Link to album artwork (stored as `TEXT` in the schema). |
| `createdAt` | `created_at` | `LocalDateTime` | When the record was created. |

### Constructor behavior

`Album` has a no-args constructor (required for JPA) plus a convenience
constructor:

```java
new Album(externalId, artistId, title, releaseYear, artworkUrl)
```

This constructor automatically sets `createdAt` to `LocalDateTime.now()` at
object-creation time — callers do not need to (and should not) set it
themselves. The `id` is left unset and is populated by the database on save.

### Relationship to Artist

An `Album` belongs to one `Artist` via `artistId`. This is an
application-level relationship only: the entity does not declare a JPA
`@ManyToOne` association, so Hibernate will not cascade, fetch, or validate
the related `Artist` for you. Code that creates an `Album` must ensure the
`Artist` it references has already been persisted and pass its id in.

Other entities that reference an album (`Song.albumId`, `Review.albumId`,
`Comment.albumId`) follow the same plain-foreign-key pattern, so an album's
songs/reviews/comments must be looked up through their own repositories —
`Album` does not expose them directly.

### Known limitations / edge cases

- The Flyway schema does not mark any `albums` column `NOT NULL`, and the
  entity has no `@Column(nullable = ...)` annotations, so all fields
  (including `title` and `artistId`) are optional at the persistence layer —
  validation, if required, must happen before an `Album` is saved.
- `artist_id` is a plain `BIGINT` column with no foreign-key constraint or
  JPA relation, so it is possible to persist an `Album` referencing an
  `artistId` that does not correspond to any existing `Artist` row.

### History

Earlier versions of this entity only mapped the `id` column, even though the
Flyway migration already defined the remaining columns — the entity has
since been brought in line with the schema (mirroring the pattern used by
`Song`).
