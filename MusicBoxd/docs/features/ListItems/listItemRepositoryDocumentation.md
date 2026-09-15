## List Item Repository

`ListItemRepository` (`com.example.MusicBoxd.Repository.ListItemRepository`) is the
data-access interface for the [`ListItem` model](./listItemModelDocumentation.md). It
declares no additional query methods of its own — every operation it exposes is the
standard `CrudRepository` contract.

> **Known issue:** `ListItemRepository` is currently declared as
> `CrudRepository<ListItem, Long>` in source, even though `ListItem.id` is an
> `Integer` (see the [`ListItem` model docs](./listItemModelDocumentation.md)). Since
> the generic id type doesn't match the entity's actual `@Id` field type, callers of
> `findById`/`existsById`/`deleteById` must pass a `Long` (e.g.
> `listItem.getId().longValue()`) rather than the `Integer` returned by `getId()`. See
> [the repository tests](../../tests/ListItems/testListItemModel.md) for how this is
> worked around; the correct fix is to change the repository declaration to
> `CrudRepository<ListItem, Integer>`.

### What it provides

Because it only extends `CrudRepository`, `ListItemRepository` gives you, out of the
box:

- `save(ListItem item)` — insert a new list item or update an existing one (by primary key).
- `findById(Long id)` — look up a single list item, returned as `Optional<ListItem>` (see the known issue above about the `Long`/`Integer` mismatch).
- `findAll()` — retrieve every list item.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(ListItem item)`
- `count()`

There are currently no custom finder methods (e.g. no `findByListIdOrderByPosition`) —
any lookup beyond "by id" or "all" is not yet supported by this repository and would
need to be added as a derived query method or `@Query` if needed.

### Usage example

```java
@Autowired
private ListItemRepository listItemRepository;

ListItem saved = listItemRepository.save(
    new ListItem(list.getId(), album.getId(), null, 1)
);

Optional<ListItem> found = listItemRepository.findById(saved.getId().longValue());
```

### Related documentation

- [List Item model](./listItemModelDocumentation.md)
- [List Item repository tests](../../tests/ListItems/testListItemModel.md)
