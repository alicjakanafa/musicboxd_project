## Global Controller Advice Tests

`GlobalControllerAdviceTest`
(`src/test/java/com/example/MusicBoxd/Controller/GlobalControllerAdviceTest.java`) is a
plain JUnit/Mockito unit test (no Spring context) that verifies the `currentUser` model
attribute added to every MVC view by
`GlobalControllerAdvice` (`src/main/java/com/example/MusicBoxd/Controller/GlobalControllerAdvice.java`).

### Test setup

`GlobalControllerAdvice#currentUser(Authentication)` is called directly with a mocked
`UserRepository` and hand-built `Authentication` objects
(`TestingAuthenticationToken`), rather than through `MockMvc`/a full Spring context. Since
the method's entire behaviour is a few conditionals plus one repository lookup, this gives
the most direct, fastest possible coverage without needing a controller to attach a
`@WebMvcTest` slice to.

### What each test verifies

- **`returnsNullWhenAuthenticationIsNull`** — a `null` `Authentication` (the unauthenticated
  case) yields a `null` `currentUser`, and the repository is never queried.
- **`returnsNullWhenAuthenticationIsNotAuthenticated`** — an `Authentication` whose
  `isAuthenticated()` is `false` also yields `null`, again without querying the repository.
- **`returnsNullWhenAuthenticatedUserHasNoMatchingRecord`** — when authenticated but
  `UserRepository.findByOktaUserId(...)` returns `Optional.empty()` (no matching `User`
  row), `currentUser` is `null`.
- **`returnsMatchingUserWhenAuthenticatedUserIsFound`** — when
  `findByOktaUserId(...)` returns a matching `User`, that exact `User` instance is returned
  as `currentUser`.

### Why every other `@WebMvcTest` needs a `UserRepository` mock

`GlobalControllerAdvice` is a `@ControllerAdvice`, so Spring MVC loads it into **every**
`@WebMvcTest` slice regardless of which controller is under test, and its `@ModelAttribute`
method runs before every request. Any `@WebMvcTest` that does not provide a
`@MockitoBean UserRepository` will fail to start its `ApplicationContext` (no bean of type
`UserRepository` available). See `SecurityConfigTest`, `AuthControllerTest` and
`UserControllerTest` for examples.

### Running the tests

```
./mvnw -Dtest=GlobalControllerAdviceTest test
```

or, as part of the full suite:

```
./mvnw test
```
