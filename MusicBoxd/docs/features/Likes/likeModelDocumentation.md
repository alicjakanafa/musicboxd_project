## Like Model

`Like` (`com.example.MusicBoxd.Model.Like`) represents a [`User`](../Users/userModelDocumentation.md)
liking either a review or a comment. It maps to the `likes` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field        | Column        | Type            | Notes                                                                 |
|--------------|---------------|-----------------|-------------------------------------------------------------------------|
| `id`         | `id`          | `Long`          | Primary key, auto-generated (`bigserial` / `IDENTITY`).                 |
| `userId`     | `user_id`     | `Long`          | Id of the `User` who liked the item.                                    |
| `reviewId`   | `review_id`   | `Long`          | Id of the liked [`Review`](../Reviews/reviewModelDocumentation.md`), or `null` when the like targets a comment instead. |
| `commentId`  | `comment_id`  | `Long`          | Id of the liked [`Comment`](../Comments/commentModelDocumentation.md), or `null` when the like targets a review instead. |
| `createdAt`  | `created_at`  | `LocalDateTime` | When the like was created.                                              |

`userId`, `reviewId` and `commentId` are plain `Long` columns — there are no
`@ManyToOne` associations on this entity, so JPA does not enforce referential integrity
with `User`, `Review` or `Comment` at the entity level. Nothing at the entity or
database level enforces that exactly one of `reviewId`/`commentId` is set — a `Like`
could technically be persisted with both, neither, or either set; callers are expected
to populate exactly one depending on what is being liked.

### Creating a like

Besides the no-args constructor (used by JPA/Hibernate), `Like` has a constructor that
takes the user-facing fields and automatically stamps `createdAt` to "now":

```java
// Liking a review
Like reviewLike = new Like(userId, reviewId, null);

// Liking a comment
Like commentLike = new Like(userId, null, commentId);
```

`createdAt` does not need to be set manually — the constructor always populates it with
`LocalDateTime.now()` at object-creation time.

### Related documentation

- [Like Repository](./likeRepositoryDocumentation.md)
- [Like repository tests](../../tests/Likes/testLikeModel.md)
