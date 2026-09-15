## Song Model

`com.example.MusicBoxd.Model.Song` is the JPA entity representing a single
track that belongs to an album. It maps to the `songs` table, created by the
Flyway migration `V5__create_songs_table.sql`.

### Why this exists

Albums are made up of individual songs (tracks). The `Song` entity is how the
application stores and reads back per-track metadata — title, track number,
and links to streaming/artwork URLs — and how a song is associated with its
parent album.

### Fields

| Field | Column | Type | Notes |
|---|---|---|---|
| `id` | `id` | `Long` | Primary key, auto-generated (`GenerationType.IDENTITY`). |
| `externalId` | `external_id` | `String` | Identifier from an external source (e.g. a music data provider). |
| `albumId` | `album_id` | `Long` | Id of the owning `Album`. Not modeled as a JPA `@ManyToOne`/`@JoinColumn` relation — it is a plain foreign-key-style column, so callers are responsible for supplying a valid album id themselves. |
| `title` | `title` | `String` | Song title. |
| `trackNumber` | `track_number` | `Integer` | Position of the song within its album. |
| `songUrl` | `song_url` | `String` | Link to the playable song (stored as `TEXT` in the schema). |
| `songImageUrl` | `song_image_url` | `String` | Link to song/track artwork (stored as `TEXT` in the schema). |
| `createdAt` | `created_at` | `LocalDateTime` | When the record was created. |

### Constructor behavior

`Song` has a no-args constructor (required for JPA) plus a convenience
constructor:

```java
new Song(externalId, albumId, title, trackNumber, songUrl, songImageUrl)
```

This constructor automatically sets `createdAt` to `LocalDateTime.now()` at
object-creation time — callers do not need to (and should not) set it
themselves. The `id` is left unset and is populated by the database on save.

### Relationship to Album

A `Song` belongs to one `Album` via `albumId`. This is an application-level
relationship only: the entity does not declare a JPA `@ManyToOne` association,
so Hibernate will not cascade, fetch, or validate the related `Album` for you.
Code that creates a `Song` must ensure the `Album` it references has already
been persisted and pass its id in.

### Known limitations / edge cases

- The Flyway schema does not mark any `songs` column `NOT NULL`, and the
  entity has no `@Column(nullable = ...)` annotations, so all fields
  (including `title` and `albumId`) are optional at the persistence layer —
  validation, if required, must happen before a `Song` is saved.
- `album_id` is a plain `BIGINT` column with no foreign-key constraint or
  JPA relation, so it is possible to persist a `Song` referencing an
  `albumId` that does not correspond to any existing `Album` row.
