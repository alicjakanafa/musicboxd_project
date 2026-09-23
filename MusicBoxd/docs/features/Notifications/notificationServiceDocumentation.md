## Notification Service

`NotificationService` (`com.example.MusicBoxd.service.NotificationService`) is the
`@Service` that other parts of the application call to create
[`Notification`](./notificationModelDocumentation.md) rows, instead of constructing and
saving `Notification` entities directly through
[`NotificationRepository`](./notificationRepositoryDocumentation.md).

### What it provides

- `createNotification(Long userId, Long actorId, Long relatedId, String type, String notificationText)`
  — the low-level building block. Constructs a new `Notification` and saves it through
  `NotificationRepository`, **unless `userId` equals `actorId`**, in which case it does
  nothing. This self-notification guard means a user is never notified about their own
  actions (e.g. liking their own review), regardless of which higher-level method is
  used.
- `notifyFriendRequest(Long receiverId, Long requesterId, String requesterUsername)` —
  notifies `receiverId` that `requesterId` sent them a friend request. Both the "actor"
  and the "related id" on the notification are set to `requesterId`. Notification text:
  `"<requesterUsername> sent you a friend request."`. Type: `FRIEND_REQUEST`.
- `notifyFriendAccepted(Long requesterId, Long accepterId, String accepterUsername)` —
  notifies `requesterId` that `accepterId` accepted their friend request. Both the actor
  and related id are `accepterId`. Notification text:
  `"<accepterUsername> accepted your friend request."`. Type: `FRIEND_ACCEPTED`.
- `notifyFriendReviewed(Long friendId, Long reviewerId, Long reviewId, String reviewerUsername)`
  — notifies `friendId` that `reviewerId` posted a new review. The actor is `reviewerId`
  and the related id is the `reviewId`, so the notification can link to that specific
  review. Notification text: `"<reviewerUsername> posted a new review."`. Type:
  `ALBUM_REVIEWED`.
- `notifyReviewLiked(Long reviewOwnerId, Long likerId, Long reviewId, String likerUsername)`
  — notifies `reviewOwnerId` that `likerId` liked their review. The actor is `likerId`
  and the related id is the `reviewId`. Notification text:
  `"<likerUsername> liked your review."`. Type: `REVIEW_LIKED`.

Every one of the four higher-level methods delegates to `createNotification`, so the
"don't notify yourself" guard applies uniformly: liking your own review, accepting your
own friend request, etc. all silently produce no notification.

### Usage example

```java
@Autowired
private NotificationService notificationService;

// A sends B a friend request.
notificationService.notifyFriendRequest(bUserId, aUserId, "alice");

// C likes their own review — no notification is created.
notificationService.notifyReviewLiked(cUserId, cUserId, reviewId, "carol");
```

### Related documentation

- [Notification model](./notificationModelDocumentation.md)
- [Notification repository](./notificationRepositoryDocumentation.md)
- [Notification service tests](../../tests/Notifications/testNotificationService.md)
