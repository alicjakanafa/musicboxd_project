## Ticketmaster Service

`TicketmasterService` (`com.example.MusicBoxd.api.ticketmaster.TicketmasterService`) is
a `@Service` that wraps the
[Ticketmaster Discovery API](https://developer.ticketmaster.com/products-and-docs/apis/discovery-api/v2/)
so the rest of the application can look up an artist's upcoming concerts without talking
to `RestClient` directly.

### What it provides

- `getAttractionId(String artistName)` — searches Ticketmaster's `/attractions.json`
  endpoint for `artistName` and returns the Ticketmaster attraction id of the first
  attraction whose name matches `artistName` **case-insensitively**. This filtering step
  matters because Ticketmaster's keyword search can return similarly-named attractions
  (e.g. tribute bands) alongside the actual artist; only an exact (case-insensitive) name
  match is used. Returns `null` if there is no response body, no `_embedded` object, no
  `attractions` list, or no attraction whose name matches.
- `getShowsByAttractionId(String attractionId)` — looks up upcoming events for a
  Ticketmaster attraction id via `/events.json` (sorted `date,asc`) and converts each
  event into a [`Concert`](../../../src/main/java/com/example/MusicBoxd/api/ticketmaster/Concert.java)
  record (`name`, `date`, `venue`, `city`, `ticketUrl`). Returns an **empty list** (never
  `null`) if there is no response body, no `_embedded` object, or no `events` list.

### Request shape

| Param         | `getAttractionId` | `getShowsByAttractionId` |
|---------------|--------------------|----------------------------|
| Path          | `/attractions.json`| `/events.json`             |
| `keyword`     | the artist name    | —                           |
| `attractionId`| —                  | the attraction id           |
| `sort`        | —                  | `date,asc`                  |
| `apikey`      | the configured key | the configured key           |

The base URL and API key are injected via `@Value("${ticketmaster.api.base-url}")` and
`@Value("${ticketmaster.api.key}")` respectively, and used to build a single internal
`RestClient` in the constructor.

### Mapping an event to a `Concert`

For each Ticketmaster event, `getShowsByAttractionId` extracts:

- `name` and `ticketUrl` directly from the event (`name`, `url`).
- `date` from `event.dates.start.localDate`, or `null` if `dates`/`start` is missing.
- `venue` and `city` from the **first** venue in `event._embedded.venues` (if the list is
  present and non-empty); both are `null` if there is no venue, and `city` is `null` on
  its own if the venue has no `city`.

### Usage example

```java
@Autowired
private TicketmasterService ticketmasterService;

String attractionId = ticketmasterService.getAttractionId("Radiohead");
List<Concert> upcomingShows = attractionId != null
        ? ticketmasterService.getShowsByAttractionId(attractionId)
        : List.of();
```

### Related documentation

- [Ticketmaster service tests](../../tests/ExternalMusicApis/testTicketmasterService.md)
- [iTunes service](./itunesServiceDocumentation.md)
- [Last.fm service](./lastFmServiceDocumentation.md)
