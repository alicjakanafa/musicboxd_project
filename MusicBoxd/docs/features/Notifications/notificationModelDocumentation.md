## Notification Model

`Notification` (`com.example.MusicBoxd.Model.Notification`) represents an in-app
notification delivered to a [`User`](../Users/userModelDocumentation.md) — for example,
telling them another user liked their review or sent a friend request. It maps to the
`notifications` table, created in `src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field               | Column                | Type            | Notes                                                                |
|---------------------|------------------------|-----------------|-------------------------------------------------------------------|
| `id`                | `id`                   | `Long`          | Primary key, auto-generated (`bigserial` / `IDENTITY`).             |
| `userId`            | `user_id`              | `Long`          | Id of the `User` receiving the notification.                        |
| `actorId`           | `actor_id`             | `Long`          | Id of the `User` who triggered the notification (e.g. who liked/commented/requested). |
| `relatedId`         | `related_id`           | `Long`          | Id of the related entity the notification is about (e.g. a review, comment, or friend request id); its meaning depends on `type`. |
| `type`               | `type`                 | `String`        | Free-text category of the notification (e.g. `"like"`, `"comment"`, `"friend_request"`); not constrained to an enum at the entity or DB level. |
| `notificationText`  | `notification_text`    | `String`        | Human-readable text describing the notification.                    |
| `isRead`            | `is_read`               | `boolean`       | Whether the user has read the notification. Defaults to `false` when created via the constructor; the database column also defaults to `FALSE`. |
| `createdAt`         | `created_at`            | `LocalDateTime` | When the notification was created.                                  |

`userId`, `actorId` and `relatedId` are plain `Long` columns — there are no
`@ManyToOne` associations on this entity, so JPA does not enforce referential integrity
with `User` or other entities at the entity level.

`Notification` is annotated with Lombok's `@Data` in addition to `@Getter`/`@Setter`,
which also generates `equals`/`hashCode`/`toString` based on all fields.

### Creating a notification

Besides the no-args constructor (used by JPA/Hibernate), `Notification` has a
constructor that takes the user-facing fields, always initializes `isRead` to `false`,
and automatically stamps `createdAt` to "now":

```java
Notification notification = new Notification(
    userId,
    actorId,
    relatedId,
    "like",
    "Someone liked your review"
);
```

`isRead` does not need to be set manually when creating a notification — the
constructor always initializes it to `false`; a notification must be marked read
explicitly via `setIsRead(true)`. `createdAt` similarly does not need to be set
manually — it is populated with `LocalDateTime.now()` at object-creation time.

### Related documentation

- [Notification Repository](./notificationRepositoryDocumentation.md)
- [Notification repository tests](../../tests/Notifications/testNotificationModel.md)
