## Like Repository

`LikeRepository` (`com.example.MusicBoxd.Repository.LikeRepository`) is the data-access
interface for the [`Like` model](./likeModelDocumentation.md). It extends Spring Data's
`CrudRepository<Like, Long>` and declares no additional query methods of its own —
every operation it exposes is the standard `CrudRepository` contract.

### What it provides

Because it only extends `CrudRepository`, `LikeRepository` gives you, out of the box:

- `save(Like like)` — insert a new like or update an existing one (by primary key).
- `findById(Long id)` — look up a single like, returned as `Optional<Like>`.
- `findAll()` — retrieve every like.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(Like like)`
- `count()`

There are currently no custom finder methods (e.g. no `findByUserIdAndReviewId` or
`findByCommentId`) — any lookup beyond "by id" or "all" is not yet supported by this
repository and would need to be added as a derived query method or `@Query` if needed.

### Usage example

```java
@Autowired
private LikeRepository likeRepository;

Like saved = likeRepository.save(new Like(user.getId(), review.getId(), null));

Optional<Like> found = likeRepository.findById(saved.getId());
```

### Related documentation

- [Like model](./likeModelDocumentation.md)
- [Like repository tests](../../tests/Likes/testLikeModel.md)
