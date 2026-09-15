## Artist Repository

`ArtistRepository` (`com.example.MusicBoxd.Repository.ArtistRepository`) is a
Spring Data `CrudRepository<Artist, Long>`. It declares no custom query
methods — every capability it exposes comes from `CrudRepository` itself.

### What it provides

- `save(Artist)` — insert a new artist or update an existing one; returns the
  managed entity with its generated `id` populated on insert.
- `findById(Long)` — returns an `Optional<Artist>`.
- `findAll()` — returns every artist row.
- `deleteById(Long)` / `delete(Artist)` — removes an artist.
- `existsById(Long)`, `count()`, and the other standard `CrudRepository`
  operations.

### Usage

```java
@Autowired
private ArtistRepository artistRepository;

Artist saved = artistRepository.save(new Artist("Tame Impala"));
Optional<Artist> found = artistRepository.findById(saved.getId());
```

### Limitations

There are no artist-specific finder methods (e.g. no "find by name" lookup)
— callers needing that today must filter `findAll()` results in application
code, or a new derived query method would need to be added to the interface.

### Related documentation

- [Artist Model](../Artists/artistModelDocumentation.md)
- [Artist persistence tests](../../tests/Artists/testArtistModel.md)
