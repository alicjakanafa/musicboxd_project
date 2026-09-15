## List Repository

`ListRepository` (`com.example.MusicBoxd.Repository.ListRepository`) is the data-access
interface for the [`List` model](./listModelDocumentation.md). It extends Spring Data's
`CrudRepository<List, Long>` and declares no additional query methods of its own —
every operation it exposes is the standard `CrudRepository` contract.

### What it provides

Because it only extends `CrudRepository`, `ListRepository` gives you, out of the box:

- `save(List list)` — insert a new list or update an existing one (by primary key).
- `findById(Long id)` — look up a single list, returned as `Optional<List>`.
- `findAll()` — retrieve every list.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(List list)`
- `count()`

There are currently no custom finder methods (e.g. no `findByUserId`) — any lookup
beyond "by id" or "all" is not yet supported by this repository and would need to be
added as a derived query method or `@Query` if needed.

### Usage example

```java
@Autowired
private ListRepository listRepository;

com.example.MusicBoxd.Model.List saved = listRepository.save(
    new com.example.MusicBoxd.Model.List(user.getId(), "Summer Favorites", "Songs on repeat")
);

Optional<com.example.MusicBoxd.Model.List> found = listRepository.findById(saved.getId());
```

### Related documentation

- [List model](./listModelDocumentation.md)
- [List repository tests](../../tests/Lists/testListModel.md)
