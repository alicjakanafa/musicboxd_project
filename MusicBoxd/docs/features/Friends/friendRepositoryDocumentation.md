## Friend Repository

`FriendRepository` (`com.example.MusicBoxd.Repository.FriendRepository`) is the
data-access interface for the [`Friend` model](./friendModelDocumentation.md). It
extends Spring Data's `CrudRepository<Friend, Long>` and adds a number of derived
query methods and two custom `@Query` methods used for friend-request and messaging
authorization logic (mainly in `MessageController` and the friends feature).

### What it provides

In addition to the standard `CrudRepository` operations (`save`, `findById`,
`findAll`, `existsById`, `deleteById`, `count`, ...), it declares:

- `Optional<Friend> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId)` —
  looks up a friend link/request in one specific direction (requester → receiver).
- `Optional<Friend> findByReceiverIdAndRequesterId(Long receiverId, Long requesterId)` —
  the mirror lookup, in the other direction.
- `List<Friend> findByReceiverIdAndStatus(Long receiverId, String status)` — all
  friend links where the given user is the receiver, filtered by status
  (e.g. incoming pending requests).
- `List<Friend> findByRequesterIdAndStatus(Long requesterId, String status)` — all
  friend links where the given user is the requester, filtered by status.
- `List<Friend> findByStatus(String status)` — every friend link with a given status,
  regardless of who requested/received it.
- `List<Friend> findByRequesterIdAndStatusOrReceiverIdAndStatus(Long requesterId, String requesterStatus, Long receiverId, String receiverStatus)` —
  friend links matching either "requester is X with status A" or "receiver is Y with
  status B"; typically called with the same user id and status on both sides to find
  all of a user's friend links with that status regardless of direction.
- `long countByReceiverIdAndStatus(Long receiverId, String status)` /
  `long countByRequesterIdAndStatus(Long requesterId, String status)` — counts of
  friend links by receiver/requester and status (e.g. counting pending incoming
  requests for a badge).
- `boolean existsByRequesterAndReceiverAndStatus(User requester, User receiver, Friend.Status status)`
  and `boolean existsByReceiverAndRequesterAndStatus(User receiver, User requester, Friend.Status status)` —
  custom `@Query` methods that check whether a friend link exists between two
  `User`s, in the given direction, with a given `Friend.Status` (e.g. `ACCEPTED` or
  `PENDING`). `MessageController` uses them to decide whether two users may message
  each other.

  Because `Friend.status` is stored as a plain `String`, the queries compare it against
  the enum's name (`f.status = :#{#status.name()}`), so callers pass a `Friend.Status`
  value and it matches the stored text (`"PENDING"`, `"ACCEPTED"`, `"REJECTED"`).

### Usage example

```java
@Autowired
private FriendRepository friendRepository;

Friend saved = friendRepository.save(
    new Friend(requesterUser.getId(), receiverUser.getId(), "PENDING")
);

Optional<Friend> found = friendRepository.findByRequesterIdAndReceiverId(
    requesterUser.getId(), receiverUser.getId()
);

List<Friend> pendingRequestsForMe =
    friendRepository.findByReceiverIdAndStatus(currentUser.getId(), "PENDING");
```

### Related documentation

- [Friend model](./friendModelDocumentation.md)
- [Friend repository tests](../../tests/Friends/testFriendModel.md)
