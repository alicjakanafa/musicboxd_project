## Tag Repository

`TagRepository` (`com.example.MusicBoxd.Repository.TagRepository`) is a
Spring Data `CrudRepository<Tag, Long>`. It declares no custom query
methods — it provides only the standard CRUD operations inherited from
`CrudRepository`.

### What it provides

- `save(Tag tag)` — inserts a new tag (generating its `id`) or updates an
  existing one.
- `findById(Long id)` — returns an `Optional<Tag>`.
- `findAll()` — returns every `Tag` row.
- `deleteById(Long id)` / `delete(Tag tag)` — removes a tag.
- `existsById`, `count`, and the other default `CrudRepository` methods.

### How to use it

```java
@Autowired
private TagRepository tagRepository;

Tag saved = tagRepository.save(new Tag("Rock"));
Optional<Tag> found = tagRepository.findById(saved.getId());
List<Tag> all = (List<Tag>) tagRepository.findAll();
tagRepository.deleteById(saved.getId());
```

### Limitations

Because there are no custom finder methods (e.g. `findByName`), looking up a
tag by its label currently requires fetching all tags and filtering in
application code, or adding a derived query method to the interface.
