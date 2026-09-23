## Ticketmaster Service Tests

`TicketmasterServiceTest`
(`src/test/java/com/example/MusicBoxd/api/ticketmaster/TicketmasterServiceTest.java`) is
a plain JUnit unit test (no Spring context) that verifies both public methods of
[`TicketmasterService`](../../features/ExternalMusicApis/ticketmasterServiceDocumentation.md)
build the correct outgoing request and correctly handle realistic, partial, and
malformed Ticketmaster Discovery API responses.

### Test setup

`TicketmasterService` builds its own private `RestClient` internally from constructor
arguments (`baseUrl`, `apiKey`) rather than accepting a client via dependency injection,
so the test:

1. Instantiates `TicketmasterService` directly via its constructor.
2. Builds a separate `RestClient.Builder`, binds a `MockRestServiceServer` to it with
   `MockRestServiceServer.bindTo(builder).build()`, and builds a `RestClient` from that
   same builder.
3. Replaces the service's private `restClient` field with that mock-backed client using
   `ReflectionTestUtils.setField(...)`.
4. Stubs the expected outgoing request and its response with `.expect(requestTo(...))` /
   `.andRespond(withSuccess(...))`.

No real network call to `app.ticketmaster.com` (or the test's fake base URL) is ever
made.

### What each test verifies

**`getAttractionId`**

- **`getAttractionIdReturnsIdWhenAttractionNameMatchesExactly`** — a GET to
  `/attractions.json` with `keyword=Radiohead` and `apikey=<key>` returns the id of the
  attraction whose name matches `"Radiohead"` exactly, ignoring a second, differently
  named attraction in the same response (`"Radiohead Tribute Band"`).
- **`getAttractionIdIsCaseInsensitiveWhenMatchingArtistName`** — an attraction named
  `"RADIOHEAD"` still matches a search for `"radiohead"`.
- **`getAttractionIdReturnsNullWhenNoAttractionNameMatches`** — if no attraction in the
  response matches the searched name, `getAttractionId` returns `null` rather than a
  wrong match.
- **`getAttractionIdReturnsNullWhenEmbeddedIsMissing`** — a response body of `{}` (no
  `_embedded` at all) returns `null`.
- **`getAttractionIdReturnsNullWhenAttractionsListIsMissing`** — `_embedded` present but
  with no `attractions` field also returns `null`.
- **`getAttractionIdReturnsNullWhenResponseBodyIsEmpty`** — an empty HTTP response body
  returns `null`.

**`getShowsByAttractionId`**

- **`getShowsByAttractionIdReturnsConcertsWithVenueCityAndDate`** — a GET to
  `/events.json` with `attractionId=<id>`, `sort=date,asc`, and `apikey=<key>` converts a
  full event (with dates, venue, and city) into a `Concert` with all five fields
  (`name`, `date`, `venue`, `city`, `ticketUrl`) populated correctly.
- **`getShowsByAttractionIdHandlesMissingVenueAndDateGracefully`** — an event with no
  `dates` and no `_embedded` venues maps to a `Concert` with `date`, `venue`, and `city`
  all `null`, without throwing.
- **`getShowsByAttractionIdReturnsEmptyListWhenNoEventsPresent`** — `_embedded.events: []`
  returns an empty list.
- **`getShowsByAttractionIdReturnsEmptyListWhenEmbeddedIsMissing`** — a response body of
  `{}` returns an empty list (never `null`).
- **`getShowsByAttractionIdPropagatesExceptionOnServerError`** — an HTTP 500 response
  causes `getShowsByAttractionId` to throw an `HttpServerErrorException` (unlike
  `getAttractionId`, this method does not swallow transport-level errors).
- **`getAttractionIdReturnsNullWhenResponseBodyIsEmpty`** /
  **`getShowsByAttractionIdReturnsEmptyListWhenResponseBodyIsEmpty`** — an empty HTTP
  response body is handled the same as a `null`/missing `_embedded` object.
- **`getShowsByAttractionIdReturnsEmptyListWhenEventsFieldIsExplicitlyNull`** — an
  `_embedded.events` field that is explicitly JSON `null` (rather than absent or an
  empty array) is also treated as "no events".
- **`getShowsByAttractionIdLeavesCityNullWhenVenueHasNoCity`** — a venue with a `name`
  but no `city` object maps to a `Concert` with `venue` populated and `city` left `null`.
- **`getShowsByAttractionIdTreatsEmptyVenuesListAsNoVenue`** /
  **`getShowsByAttractionIdTreatsExplicitlyNullVenuesListAsNoVenue`** — an event whose
  `_embedded.venues` is an empty list, or explicitly `null`, maps to a `Concert` with
  both `venue` and `city` left `null`.
- **`getShowsByAttractionIdLeavesDateNullWhenStartDateIsMissing`** — an event with a
  `dates` object present but `dates.start` explicitly `null` maps to a `Concert` with
  `date` left `null`.

### Running the tests

```
./mvnw -Dtest=TicketmasterServiceTest test
```

or, as part of the full suite:

```
./mvnw test
```
