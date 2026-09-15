## Notification Repository

`NotificationRepository` (`com.example.MusicBoxd.Repository.NotificationRepository`) is
the data-access interface for the
[`Notification` model](./notificationModelDocumentation.md). It extends Spring Data's
`CrudRepository<Notification, Long>` and declares no additional query methods of its
own — every operation it exposes is the standard `CrudRepository` contract.

### What it provides

Because it only extends `CrudRepository`, `NotificationRepository` gives you, out of
the box:

- `save(Notification notification)` — insert a new notification or update an existing one (by primary key).
- `findById(Long id)` — look up a single notification, returned as `Optional<Notification>`.
- `findAll()` — retrieve every notification.
- `existsById(Long id)`
- `deleteById(Long id)` / `delete(Notification notification)`
- `count()`

There are currently no custom finder methods (e.g. no `findByUserIdAndIsReadFalse`) —
any lookup beyond "by id" or "all" is not yet supported by this repository and would
need to be added as a derived query method or `@Query` if needed.

### Usage example

```java
@Autowired
private NotificationRepository notificationRepository;

Notification saved = notificationRepository.save(
    new Notification(user.getId(), actor.getId(), review.getId(), "like", "Someone liked your review")
);

Optional<Notification> found = notificationRepository.findById(saved.getId());
```

### Related documentation

- [Notification model](./notificationModelDocumentation.md)
- [Notification repository tests](../../tests/Notifications/testNotificationModel.md)
