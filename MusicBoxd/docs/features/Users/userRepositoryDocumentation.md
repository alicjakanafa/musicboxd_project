## User Repository

`UserRepository` (`com.example.MusicBoxd.Repository.UserRepository`) is the data-access
interface for the [`User` model](./userModelDocumentation.md). It extends Spring Data's
`CrudRepository<User, Long>` and declares no additional query methods of its own —
every operation it exposes is the standard `CrudRepository` contract.

### What it provides

Because it only extends `CrudRepository`, `UserRepository` gives you, out of the box:

- `save(User user)` — insert a new user or update an existing one (by primary key).
- `findById(Long id)` — look up a single user, returned as `Optional<User>`.
- `findAll()` — retrieve every user.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(User user)`
- `count()`

There are currently no custom finder methods (e.g. no `findByUsername` or
`findByGoogleUserId`) — any lookup beyond "by id" or "all" is not yet supported by this
repository and would need to be added as a derived query method or `@Query` if needed.

### Usage example

```java
@Autowired
private UserRepository userRepository;

User saved = userRepository.save(
    new User("google-123", "listener1", "listener1@example.com", "bio text", null)
);

Optional<User> found = userRepository.findById(saved.getId());
```

### Related documentation

- [User model](./userModelDocumentation.md)
- [User repository tests](../../tests/Users/testUserModel.md)
