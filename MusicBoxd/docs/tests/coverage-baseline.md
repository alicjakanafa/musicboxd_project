## Test Coverage Baseline

Coverage is measured with JaCoCo (`jacoco-maven-plugin` in `pom.xml`). Running

```bash
./mvnw clean verify
```

runs the test suite and writes the HTML report to
`target/site/jacoco/index.html` (plus `jacoco.csv` / `jacoco.xml`).

### Coverage gate

The `check` execution enforces a bundle-wide minimum of **90% line** and
**75% branch** coverage. While the suite is being built up, the gate is
non-blocking (`<jacoco.haltOnFailure>false</jacoco.haltOnFailure>` in
`pom.xml`): violations are logged as warnings. Once the target is reached,
set it to `true` so any drop below the threshold fails the build.

### Exclusions

- `MusicBoxdApplication`: `main` method only.
- `Config/EarlyDotenvRunListener`: loads `.env` during application bootstrap.
- Lombok-generated code: `lombok.config` sets
  `lombok.addLombokGeneratedAnnotation = true`, so generated getters, setters
  and constructors are marked `@Generated` and ignored by JaCoCo.

### Test profiles

| Profile | Used by | Datasource |
|---------|---------|------------|
| `datajpatest` | `@DataJpaTest` repository tests | In-memory H2, Flyway disabled |
| `apptest` | `@SpringBootTest` full-context tests | In-memory H2, Flyway disabled, dummy API / Auth0 credentials |

Full-context tests also import
`src/test/java/com/example/MusicBoxd/support/TestOAuth2Config.java`, which
provides a static OAuth2 client registration so the application does not
attempt OIDC discovery against Auth0 at startup.

### Baseline (2026-09-23, branch `tests/phase-0-tooling`)

**Total: 11.0% line (291 / 2636), 0.6% branch (4 / 628)**

| Package | Lines covered | Line coverage |
|---------|---------------|---------------|
| `Controller` | 74 / 1999 | 3.7% |
| `api.lastfm` | 49 / 220 | 22.3% |
| `Model` | 102 / 115 | 88.7% |
| `api.itunes` | 36 / 91 | 39.6% |
| `api.spotify` | 0 / 89 | 0.0% |
| `api.ticketmaster` | 6 / 75 | 8.0% |
| `Config` | 21 / 31 | 67.7% |
| `service` | 3 / 16 | 18.8% |

### Known failing test at baseline

`LastFmServiceTest.getTopArtistsBuildsExpectedRequestAndParsesResponse`
fails because `LastFmImage.getText()` returns `null`: the `#text` JSON field
is not being deserialized. This failure was already present before coverage
tooling was added and points to a defect in production code, not in the test.
The baseline numbers above were generated with
`-Dmaven.test.failure.ignore=true`.
