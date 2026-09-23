## Want To Listen Repository

`WantToListenRepository` (`com.example.MusicBoxd.Repository.WantToListenRepository`) is
the data-access interface for the
[`WantToListen` model](./wantToListenModelDocumentation.md). Besides the standard
`CrudRepository` contract, it declares one derived query method,
`findAllByUserIdOrderByCreatedAtDesc(Long id)` — see the known issue below, though,
before relying on it.

> **Note:** the `WANT_TO_LISTEN` table backing this entity was dropped in
> `src/main/resources/db/migration/V23__delete_want_to_listen_table.sql`, and this
> repository/entity are not referenced anywhere in `src/main/java` outside of tests.
> This appears to be a leftover, superseded feature (see the
> [`Favourites` feature](../Favourites/userFavouriteAlbumModelDocumentation.md) for
> similar-in-spirit functionality that is actively used).

> **Known issue:** `findAllByUserIdOrderByCreatedAtDesc` is declared to return a
> single `Optional<WantToListen>`, even though its name ("findAllBy...") and
> `OrderByCreatedAtDesc` clause both imply it should return every want-to-listen entry
> for a user, most recent first. Because the return type is a single `Optional` and
> there's no `Top`/`First`/`Pageable` limiting the query, Spring Data executes it as a
> single-result query with no `LIMIT`. As soon as a user has more than one
> want-to-listen entry, this throws
> `org.springframework.dao.IncorrectResultSizeDataAccessException` ("Query did not
> return a unique result: 2 results were returned") instead of returning anything. See
> `findAllByUserIdOrderByCreatedAtDescThrowsWhenUserHasMoreThanOneEntry` in
> [the repository tests](../../tests/WantToListen/testWantToListenModel.md) for a
> reproduction. Since the method isn't called from any production code path today,
> this is a latent rather than an active defect.

> **Known issue:** `WantToListenRepository` is currently declared as
> `CrudRepository<WantToListen, Long>` in source, even though `WantToListen.id` is an
> `Integer` (see the [`WantToListen` model docs](./wantToListenModelDocumentation.md)).
> Since the generic id type doesn't match the entity's actual `@Id` field type, callers
> of `findById`/`existsById`/`deleteById` must pass a `Long` (e.g.
> `entry.getId().longValue()`) rather than the `Integer` returned by `getId()`. See
> [the repository tests](../../tests/WantToListen/testWantToListenModel.md) for how
> this is worked around; the correct fix is to change the repository declaration to
> `CrudRepository<WantToListen, Integer>`.

### What it provides

Because it only extends `CrudRepository`, `WantToListenRepository` gives you, out of
the box:

- `save(WantToListen entry)` — insert a new want-to-listen entry or update an existing one (by primary key).
- `findById(Long id)` — look up a single entry, returned as `Optional<WantToListen>` (see the known issue above about the `Long`/`Integer` mismatch).
- `findAll()` — retrieve every entry.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(WantToListen entry)`
- `count()`

### Usage example

```java
@Autowired
private WantToListenRepository wantToListenRepository;

WantToListen saved = wantToListenRepository.save(
    new WantToListen(user.getId(), null, album.getId())
);

Optional<WantToListen> found = wantToListenRepository.findById(saved.getId().longValue());
```

### Related documentation

- [Want To Listen model](./wantToListenModelDocumentation.md)
- [Want To Listen repository tests](../../tests/WantToListen/testWantToListenModel.md)
