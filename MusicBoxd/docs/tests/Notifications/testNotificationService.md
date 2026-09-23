## Notification Service Tests

`NotificationServiceTest`
(`src/test/java/com/example/MusicBoxd/service/NotificationServiceTest.java`) is a plain
JUnit + Mockito unit test (no Spring context) that verifies
[`NotificationService`](../../features/Notifications/notificationServiceDocumentation.md)
builds the correct `Notification` for each notification type and correctly suppresses
self-notifications.

### Test setup

`NotificationRepository` is mocked with Mockito
(`mock(NotificationRepository.class)`) and injected into a real `NotificationService`
instance via its constructor. No database or Spring context is involved — each test
verifies the `Notification` passed to `notificationRepository.save(...)` using an
`ArgumentCaptor<Notification>`, or verifies that `save` was never called.

### What each test verifies

- **`createNotificationSavesNotificationWithGivenFields`** — `createNotification(...)`
  saves a `Notification` whose `userId`, `actorId`, `relatedId`, `type`, and
  `notificationText` exactly match the arguments passed in.
- **`createNotificationDoesNotSaveWhenUserIsActingOnThemselves`** — when `userId` equals
  `actorId`, `createNotification` never calls `notificationRepository.save`.
- **`notifyFriendRequestCreatesNotificationWithRequesterAsActorAndRelatedId`** —
  `notifyFriendRequest(receiverId, requesterId, "alice")` saves a notification addressed
  to `receiverId`, with `actorId` and `relatedId` both set to `requesterId`, type
  `FRIEND_REQUEST`, and text `"alice sent you a friend request."`.
- **`notifyFriendRequestDoesNothingWhenReceiverIsRequester`** — when the receiver and
  requester are the same user, no notification is saved.
- **`notifyFriendAcceptedCreatesNotificationWithAccepterAsActorAndRelatedId`** —
  `notifyFriendAccepted(requesterId, accepterId, "bob")` saves a notification addressed
  to `requesterId`, with `actorId`/`relatedId` set to `accepterId`, type
  `FRIEND_ACCEPTED`, and text `"bob accepted your friend request."`.
- **`notifyFriendReviewedCreatesNotificationReferencingReview`** —
  `notifyFriendReviewed(friendId, reviewerId, reviewId, "carol")` saves a notification
  addressed to `friendId`, with `actorId=reviewerId`, `relatedId=reviewId`, type
  `ALBUM_REVIEWED`, and text `"carol posted a new review."`.
- **`notifyFriendReviewedDoesNothingWhenReviewerIsFriend`** — when the "friend" being
  notified is also the reviewer, no notification is saved.
- **`notifyReviewLikedCreatesNotificationReferencingReview`** —
  `notifyReviewLiked(reviewOwnerId, likerId, reviewId, "dave")` saves a notification
  addressed to `reviewOwnerId`, with `actorId=likerId`, `relatedId=reviewId`, type
  `REVIEW_LIKED`, and text `"dave liked your review."`.
- **`notifyReviewLikedDoesNothingWhenOwnerLikesTheirOwnReview`** — when the review owner
  and the liker are the same user, no notification is saved.

### Running the tests

```
./mvnw -Dtest=NotificationServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
