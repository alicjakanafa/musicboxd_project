## Comment Repository

`CommentRepository` (`com.example.MusicBoxd.Repository.CommentRepository`) is
a Spring Data `CrudRepository<Comment, Long>`. It declares no custom query
methods — it provides only the standard CRUD operations inherited from
`CrudRepository`.

### What it provides

- `save(Comment comment)` — insert a new comment or update an existing one.
- `findById(Long id)` — look up a single comment by its primary key.
- `findAll()` — list every comment.
- `deleteById(Long id)` / `delete(Comment comment)` — remove a comment.
- `existsById`, `count`, and the other standard `CrudRepository` operations.

### Usage example

```java
Comment comment = new Comment(userId, reviewId, "Great review!");
Comment saved = commentRepository.save(comment);

Optional<Comment> found = commentRepository.findById(saved.getId());

commentRepository.deleteById(saved.getId());
```

### Notes

Because `Comment.userId` and `Comment.reviewId` are plain columns rather than
JPA relationships, `CommentRepository` cannot fetch a comment's related
`User`/`Review` for you (e.g. no `findByUserId` or join-fetch method exists
today). Callers that need the related entities must look them up separately
via `UserRepository`/`ReviewRepository` using the stored ids.
