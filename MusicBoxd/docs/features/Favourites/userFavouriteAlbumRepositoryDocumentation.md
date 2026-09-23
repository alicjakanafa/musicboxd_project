## User Favourite Album Repository

`UserFavouriteAlbumRepository`
(`com.example.MusicBoxd.Repository.UserFavouriteAlbumRepository`) is the data-access
interface for the
[`UserFavouriteAlbum` model](./userFavouriteAlbumModelDocumentation.md). It extends
Spring Data's `CrudRepository<UserFavouriteAlbum, Long>` and adds two derived query
methods.

### What it provides

In addition to the standard `CrudRepository` operations (`save`, `findById`, `findAll`,
`existsById`, `deleteById`, `count`, ...), it declares:

- `List<UserFavouriteAlbum> findByUserIdOrderByPositionAsc(Long userId)` — returns all
  of a user's favourite albums, ordered by their `position` ascending (i.e. in display
  order). Returns an empty list if the user has no favourites.
- `Optional<UserFavouriteAlbum> findByUserIdAndAlbumId(Long userId, Long albumId)` —
  looks up a specific user's favourite entry for a specific album, e.g. to check
  whether an album is already favourited before adding it again. Returns
  `Optional.empty()` if there is no such favourite.

### Usage example

```java
@Autowired
private UserFavouriteAlbumRepository userFavouriteAlbumRepository;

UserFavouriteAlbum saved = userFavouriteAlbumRepository.save(
    new UserFavouriteAlbum(user.getId(), album.getId(), 0)
);

List<UserFavouriteAlbum> topFour =
    userFavouriteAlbumRepository.findByUserIdOrderByPositionAsc(user.getId());

boolean alreadyFavourited =
    userFavouriteAlbumRepository.findByUserIdAndAlbumId(user.getId(), album.getId())
        .isPresent();
```

### Related documentation

- [User Favourite Album model](./userFavouriteAlbumModelDocumentation.md)
- [User Favourite Album repository tests](../../tests/Favourites/testUserFavouriteAlbumModel.md)
