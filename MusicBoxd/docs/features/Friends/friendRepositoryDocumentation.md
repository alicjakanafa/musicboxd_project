## Friend Repository

`FriendRepository` (`com.example.MusicBoxd.Repository.FriendRepository`) is the
data-access interface for the [`Friend` model](./friendModelDocumentation.md). It
extends Spring Data's `CrudRepository<Friend, Long>` and declares no additional query
methods of its own — every operation it exposes is the standard `CrudRepository`
contract.

### What it provides

Because it only extends `CrudRepository`, `FriendRepository` gives you, out of the box:

- `save(Friend friend)` — insert a new friend request/link or update an existing one (by primary key).
- `findById(Long id)` — look up a single friend record, returned as `Optional<Friend>`.
- `findAll()` — retrieve every friend record.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(Friend friend)`
- `count()`

There are currently no custom finder methods (e.g. no `findByRequesterIdAndReceiverId`
or `findByReceiverIdAndStatus`) — any lookup beyond "by id" or "all" is not yet
supported by this repository and would need to be added as a derived query method or
`@Query` if needed.

### Usage example

```java
@Autowired
private FriendRepository friendRepository;

Friend saved = friendRepository.save(
    new Friend(requesterUser.getId(), receiverUser.getId(), "pending")
);

Optional<Friend> found = friendRepository.findById(saved.getId());
```

### Related documentation

- [Friend model](./friendModelDocumentation.md)
- [Friend repository tests](../../tests/Friends/testFriendModel.md)
