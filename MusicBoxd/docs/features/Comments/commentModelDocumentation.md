## Comment Model

`Comment` (`com.example.MusicBoxd.Model.Comment`) represents a single comment a
user leaves on a review. It is a JPA entity mapped to the `COMMENTS` table.

### Why / when this exists

Comments let users respond to each other's reviews (e.g. agreeing, adding
context, or discussing a rating). Each comment is tied to the user who wrote
it and the review it was written on.

### Fields

| Field       | Column       | Type            | Notes                                                                 |
|-------------|--------------|-----------------|------------------------------------------------------------------------|
| `id`        | `id`         | `Long`          | Primary key, auto-generated (`IDENTITY`).                             |
| `userId`    | `user_id`    | `Long`          | Id of the `User` who wrote the comment.                               |
| `reviewId`  | `review_id`  | `Long`          | Id of the `Review` the comment was posted on.                         |
| `content`   | `content`    | `String`        | The comment text.                                                     |
| `createdAt` | `created_at` | `LocalDateTime` | Timestamp the comment was created. Set automatically in the app-level constructor. |

A convenience constructor `Comment(Long userId, Long reviewId, String content)`
sets `createdAt` to `LocalDateTime.now()` at construction time; a no-args
constructor is also available (via Lombok `@NoArgsConstructor`) for JPA and
manual field assembly.

### Relationships

`userId` and `reviewId` relate `Comment` to `User` and `Review` respectively,
but only as plain `Long` columns — there is no `@ManyToOne`/`@JoinColumn`
mapping today, so related rows are not navigable from a `Comment` object and
referential integrity between these tables is not enforced by JPA/Hibernate.

### Known limitations

The underlying `comments` table (see `V7__create_comments_table.sql`) allows
`user_id`, `review_id` and `content` to be `NULL` at the database level, and
the entity does not add `@Column(nullable = false)` constraints of its own —
so the model does not currently guarantee a comment is linked to a user or
review. Callers are responsible for populating these fields correctly.
