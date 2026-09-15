## Review Repository

`ReviewRepository` (`com.example.MusicBoxd.Repository.ReviewRepository`) is the persistence
access point for [[reviewModelDocumentation|Review]] entities.

### What it provides

```java
public interface ReviewRepository extends CrudRepository<Review, Long> {
}
```

It declares no custom query methods — it exposes exactly the standard
`CrudRepository<Review, Long>` operations:

- `save(Review review)` — insert a new review or update an existing one (matched by `id`).
- `findById(Long id)` — look up a single review, returned as `Optional<Review>`.
- `findAll()` — return every review in the table.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(Review review)`
- `count()`

### How to use it

```java
@Autowired
private ReviewRepository reviewRepository;

Review saved = reviewRepository.save(
    new Review(userId, albumId, songId, "Header", "Body text", new BigDecimal("4.0"))
);

Optional<Review> found = reviewRepository.findById(saved.getId());

List<Review> all = (List<Review>) reviewRepository.findAll();

reviewRepository.deleteById(saved.getId());
```

Note that `findAll()` returns an `Iterable<Review>` per the `CrudRepository` contract; cast
to `List` (as shown above and in the tests) when list-specific operations are needed.

### Custom query methods

None currently. If a feature needs to look up reviews by `userId`, `albumId`, or `songId`
(e.g. "all reviews for this album"), that would be a new derived query method added to this
interface (e.g. `List<Review> findByAlbumId(Long albumId)`) — no such method exists yet.

### Related docs

- [[reviewModelDocumentation]] — the `Review` entity this repository manages.
- [[testReviewModel]] — the test suite covering this repository's behavior.
