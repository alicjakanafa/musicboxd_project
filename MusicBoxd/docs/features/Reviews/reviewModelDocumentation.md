## Review Model

`Review` (`com.example.MusicBoxd.Model.Review`) represents a user's written review of an
album, or optionally of a specific song on an album, including a numeric rating.

### Why / when it's used

A review is created whenever a user rates and writes up their thoughts on an album (or a
particular track). It's the core content type the "boxd"-style rating/review experience is
built around — one row per review, always tied back to the reviewing user and the
album being reviewed.

### Table

Maps to the `reviews` table (Flyway migration `V6__create_reviews_table.sql`).

### Fields

| Field       | Column       | Type            | Notes                                                                 |
|-------------|-------------|-----------------|------------------------------------------------------------------------|
| `id`        | `id`        | `Long`          | Primary key, auto-generated (`IDENTITY`).                              |
| `userId`    | `user_id`   | `Long`          | Id of the reviewing `User`. Not enforced as a foreign key at the entity level — see Limitations. |
| `albumId`   | `album_id`  | `Long`          | Id of the `Album` being reviewed.                                      |
| `songId`    | `song_id`   | `Long`          | Id of the specific `Song` being reviewed, or `null` for an album-level review. |
| `header`    | `header`    | `String`        | Short title/headline for the review (column is `VARCHAR(100)`).        |
| `content`   | `content`   | `String`        | Full review body text (`TEXT` column, unbounded length).               |
| `rating`    | `Rating`    | `BigDecimal`    | Numeric rating, stored as `DECIMAL(2,1)` (one decimal place, e.g. `4.5`). |
| `createdAt` | `created_at`| `LocalDateTime` | Set automatically to "now" by the all-args constructor when a `Review` is created in application code; defaults to `CURRENT_TIMESTAMP` at the database level if inserted without a value. |

### Relationships

`Review` links to three other entities purely by id column (`userId`, `albumId`, `songId`) —
there are no `@ManyToOne`/`@JoinColumn` object-graph relationships on the entity, so callers
must resolve/join to `User`, `Album`, or `Song` manually when needed.

- **User → Review**: every review belongs to exactly one user (`userId`).
- **Album → Review**: every review belongs to exactly one album (`albumId`).
- **Song → Review**: optional. When `songId` is present, the review is about that specific
  track; when `songId` is `null`, the review is about the album as a whole.

### Example

```java
Review review = new Review(
    user.getId(),
    album.getId(),
    null,                 // album-level review, no specific song
    "Great album",
    "Loved every track.",
    new BigDecimal("4.5")
);
Review saved = reviewRepository.save(review);
```

### Limitations / edge cases

- The `id_user`, `album_id`, and `song_id` columns are plain `BIGINT`s with no foreign key
  or `NOT NULL` constraint declared in the migration, and no corresponding validation on the
  entity — the application layer is responsible for ensuring these ids reference real rows.
- `rating` has no validation on the entity (e.g. no `@DecimalMin`/`@DecimalMax`); the only
  range constraint is the database column's `DECIMAL(2,1)` precision, which limits values to
  a single digit before and after the decimal point (e.g. `9.9` max).
- Comparing `rating` values should use `BigDecimal.compareTo` / `isEqualByComparingTo`
  rather than `equals`, since scale can differ between an in-memory value and one read back
  from the database (see [[testReviewModel]]).
