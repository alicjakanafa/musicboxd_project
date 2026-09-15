## Message Repository

`MessageRepository` (`com.example.MusicBoxd.Repository.MessageRepository`) is the
data-access interface for the [`Message` model](./messageModelDocumentation.md). It
extends Spring Data's `CrudRepository<Message, Long>` and declares no additional query
methods of its own — every operation it exposes is the standard `CrudRepository`
contract.

### What it provides

Because it only extends `CrudRepository`, `MessageRepository` gives you, out of the
box:

- `save(Message message)` — insert a new message or update an existing one (by primary key).
- `findById(Long id)` — look up a single message, returned as `Optional<Message>`.
- `findAll()` — retrieve every message.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(Message message)`
- `count()`

There are currently no custom finder methods (e.g. no
`findBySenderIdAndReceiverIdOrderByCreatedAt`) — any lookup beyond "by id" or "all" is
not yet supported by this repository and would need to be added as a derived query
method or `@Query` if needed.

### Usage example

```java
@Autowired
private MessageRepository messageRepository;

Message saved = messageRepository.save(
    new Message(sender.getId(), receiver.getId(), "Hey!", null, null, null, null)
);

Optional<Message> found = messageRepository.findById(saved.getId());
```

### Related documentation

- [Message model](./messageModelDocumentation.md)
- [Message repository tests](../../tests/Messages/testMessageModel.md)
