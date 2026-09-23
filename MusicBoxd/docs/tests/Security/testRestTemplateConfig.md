## Rest Template Config Tests

`RestTemplateConfigTest`
(`src/test/java/com/example/MusicBoxd/Config/RestTemplateConfigTest.java`) is a plain
JUnit unit test that verifies `RestTemplateConfig`
(`src/main/java/com/example/MusicBoxd/Config/RestTemplateConfig.java`)'s `restTemplate()`
`@Bean` method produces a usable `RestTemplate`.

### What the test verifies

- **`restTemplateBeanProducesAUsableRestTemplateInstance`** — calling `restTemplate()`
  returns a non-null `RestTemplate` instance, and each direct call to the `@Bean` method
  produces a distinct instance (singleton scoping is Spring's container's responsibility,
  not the method's own).

### Running the tests

```
./mvnw -Dtest=RestTemplateConfigTest test
```

or, as part of the full suite:

```
./mvnw test
```
