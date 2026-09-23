## Web Config Tests

`WebConfigTest` (`src/test/java/com/example/MusicBoxd/Config/WebConfigTest.java`) is a
plain JUnit unit test (no Spring context) that verifies `WebConfig`
(`src/main/java/com/example/MusicBoxd/Config/WebConfig.java`) registers the resource
handler that serves user-uploaded files.

### Test setup

The test builds a real `ResourceHandlerRegistry` (backed by a `StaticWebApplicationContext`
and a `MockServletContext`) and calls `WebConfig#addResourceHandlers(...)` against it
directly - no `@WebMvcTest`/`MockMvc` needed. `ResourceHandlerRegistry` only exposes
registered patterns publicly via `hasMappingForPattern(...)`; the underlying handler (and
therefore the configured resource location) is only reachable through its
package-private `getHandlerMapping()` method, so the test uses reflection purely to read
that already-computed, already-public configuration value.

See [`testAuthController.md`](testAuthController.md) for an end-to-end check (through
`MockMvc`) that a real file placed under `uploads/` is actually served at `/uploads/**`.

### What the test verifies

- **`registersUploadsResourceHandlerBackedByTheUploadsDirectory`** — after calling
  `addResourceHandlers(...)`, the registry has a mapping for `/uploads/**`, and the
  resulting `ResourceHttpRequestHandler`'s resolved location resource description contains
  `"uploads"` (i.e. it points at the project's `uploads/` directory, matching
  `file:uploads/`).

### Running the tests

```
./mvnw -Dtest=WebConfigTest test
```

or, as part of the full suite:

```
./mvnw test
```
