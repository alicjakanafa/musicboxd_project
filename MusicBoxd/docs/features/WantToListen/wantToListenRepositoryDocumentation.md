## Want To Listen Repository

`WantToListenRepository` (`com.example.MusicBoxd.Repository.WantToListenRepository`) is
the data-access interface for the
[`WantToListen` model](./wantToListenModelDocumentation.md). It declares no additional
query methods of its own — every operation it exposes is the standard `CrudRepository`
contract.

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

There are currently no custom finder methods (e.g. no `findByUserId`) — any lookup
beyond "by id" or "all" is not yet supported by this repository and would need to be
added as a derived query method or `@Query` if needed.

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
