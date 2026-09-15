## Message Model

`Message` (`com.example.MusicBoxd.Model.Message`) represents a direct message sent
between two [`User`](../Users/userModelDocumentation.md) accounts. A message can
optionally carry an attached "song share" (title/artist/artwork/preview URL) alongside
its text content. It maps to the `messages` table, created in
`src/main/resources/db/migration/V1__init.sql`.

### Fields

| Field             | Column              | Type            | Notes                                                        |
|-------------------|---------------------|-----------------|----------------------------------------------------------------|
| `id`              | `id`                | `Long`          | Primary key, auto-generated (`serial` / `IDENTITY`).            |
| `senderId`        | `sender_id`         | `Long`          | Id of the `User` who sent the message.                          |
| `receiverId`      | `receiver_id`       | `Long`          | Id of the `User` who received the message.                      |
| `content`         | `content`           | `String`        | The message text.                                                |
| `read`            | `read`              | `boolean`       | Whether the receiver has read the message. Defaults to `false` when created via the constructor. |
| `songTitle`       | `song_title`        | `String`        | Title of an optionally-shared song. Optional.                    |
| `songArtist`      | `song_artist`       | `String`        | Artist of an optionally-shared song. Optional.                   |
| `songImageUrl`    | `song_image_url`    | `String`        | Artwork URL of an optionally-shared song. Optional.               |
| `songPreviewUrl`  | `song_preview_url`  | `String`        | Preview-audio URL of an optionally-shared song. Optional.         |
| `createdAt`       | `created_at`        | `LocalDateTime` | When the message was sent.                                       |

`senderId` and `receiverId` are plain `Long` columns — there is no `@ManyToOne`
association to `User` on this entity, so JPA does not enforce referential integrity at
the entity level. The song-share fields (`songTitle`/`songArtist`/`songImageUrl`/
`songPreviewUrl`) are plain strings, not a foreign key to `Song` — a message can
reference a song by copying its display data directly rather than linking to a `Song`
row.

### Creating a message

Besides the no-args constructor (used by JPA/Hibernate), `Message` has a constructor
that takes the user-facing fields, always initializes `read` to `false`, and
automatically stamps `createdAt` to "now":

```java
Message message = new Message(
    senderId,
    receiverId,
    "Check this track out!",
    songTitle,       // may be null if no song is shared
    songArtist,      // may be null
    songImageUrl,    // may be null
    songPreviewUrl   // may be null
);
```

`read` does not need to be set manually when creating a message — the constructor
always initializes it to `false`; a message must be marked read explicitly via
`setRead(true)`. `createdAt` similarly does not need to be set manually — it is
populated with `LocalDateTime.now()` at object-creation time.

### Related documentation

- [Message Repository](./messageRepositoryDocumentation.md)
- [Message repository tests](../../tests/Messages/testMessageModel.md)
