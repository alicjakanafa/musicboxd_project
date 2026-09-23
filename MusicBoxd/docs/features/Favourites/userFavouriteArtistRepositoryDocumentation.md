## User Favourite Artist Repository

`UserFavouriteArtistRepository`
(`com.example.MusicBoxd.Repository.UserFavouriteArtistRepository`) is the data-access
interface for the
[`UserFavouriteArtist` model](./userFavouriteArtistModelDocumentation.md). It extends
Spring Data's `CrudRepository<UserFavouriteArtist, Long>` and adds four query methods.

### What it provides

In addition to the standard `CrudRepository` operations (`save`, `findById`, `findAll`,
`existsById`, `deleteById`, `count`, ...), it declares:

- `List<UserFavouriteArtist> findByUserIdOrderByIdAsc(Long userId)` — returns all of a
  user's favourite artists, ordered by `id` ascending (effectively insertion order).
  Returns an empty list if the user has no favourite artists.
- `Optional<UserFavouriteArtist> findByUserIdAndArtistId(Long userId, Long artistId)` —
  looks up a specific user's favourite entry for a specific artist. Returns
  `Optional.empty()` if there is no such favourite.
- `boolean existsByUserIdAndArtistId(Long userId, Long artistId)` — a cheaper
  true/false check for whether a user has already favourited a given artist, useful
  for guarding against duplicate favourites before inserting a new one.
- `void deleteByUserIdAndArtistId(Long userId, Long artistId)` — removes a user's
  favourite entry for a specific artist (e.g. "unfavourite"). Only the matching
  row(s) for that user/artist pair are removed; other favourites are unaffected.

### Usage example

```java
@Autowired
private UserFavouriteArtistRepository userFavouriteArtistRepository;

if (!userFavouriteArtistRepository.existsByUserIdAndArtistId(user.getId(), artist.getId())) {
    userFavouriteArtistRepository.save(new UserFavouriteArtist(user.getId(), artist.getId()));
}

List<UserFavouriteArtist> favourites =
    userFavouriteArtistRepository.findByUserIdOrderByIdAsc(user.getId());

userFavouriteArtistRepository.deleteByUserIdAndArtistId(user.getId(), artist.getId());
```

### Related documentation

- [User Favourite Artist model](./userFavouriteArtistModelDocumentation.md)
- [User Favourite Artist repository tests](../../tests/Favourites/testUserFavouriteArtistModel.md)
