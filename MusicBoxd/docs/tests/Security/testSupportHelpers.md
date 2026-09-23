## Shared OAuth2/OIDC Test Support Helpers

Every `@WebMvcTest` that loads `SecurityConfig` needs an authenticated OIDC principal to
exercise its protected routes, and `SecurityConfig` needs a `ClientRegistrationRepository`
bean to exist just to build its `oauth2Login()` filter chain - even in tests that never
perform a real OAuth2 redirect/token exchange. Two reusable helpers under
`src/test/java/com/example/MusicBoxd/support/` remove that boilerplate from individual
controller tests.

### `TestOAuth2Config`

`TestOAuth2Config` (`src/test/java/com/example/MusicBoxd/support/TestOAuth2Config.java`) is
a `@TestConfiguration` that supplies a single dummy `ClientRegistrationRepository` bean,
registered under the id `"okta"` (`TestOAuth2Config.REGISTRATION_ID`), matching the single
provider the application integrates with. None of the URIs it uses are real; they only need
to be well-formed enough for Spring Security to build the registration and filter chain.

Import it alongside `SecurityConfig` in any `@WebMvcTest`:

```java
@WebMvcTest(controllers = SomeController.class)
@Import({SecurityConfig.class, TestOAuth2Config.class})
```

### `OidcTestUsers`

`OidcTestUsers` (`src/test/java/com/example/MusicBoxd/support/OidcTestUsers.java`) wraps
Spring Security Test's `SecurityMockMvcRequestPostProcessors.oidcLogin()` so every test
builds an equivalent authenticated principal - one whose `sub` and `email` claims line up
with what the application's controllers actually read (`GlobalControllerAdvice`,
`UserController#afterLogin`, etc.):

```java
mockMvc.perform(get("/some/protected/path")
        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
```

An overload also accepts a `picture` claim for tests that assert on the profile picture URL
propagated to a newly-created `User` (see `UserControllerTest`).

### Why a real network call had to be avoided

`okta-spring-boot-starter` registers an `EnvironmentPostProcessor`
(`OktaOAuth2PropertiesMappingEnvironmentPostProcessor`) that eagerly calls
`{okta.oauth2.issuer}/.well-known/openid-configuration` during environment preparation,
**before** any `@WebMvcTest` auto-configuration exclusions take effect. It only recovers
gracefully from `ResourceAccessException` (DNS/connection failures) or
`JsonProcessingException` - not from an HTTP error response - so pointing
`okta.oauth2.issuer` at a domain that resolves and answers with a non-2xx status (as
`https://test.okta.com/` did) crashes the test's `ApplicationContext`. Every `@WebMvcTest`
that loads `SecurityConfig` therefore sets:

```java
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
```

`.invalid` is an IANA-reserved TLD guaranteed never to resolve (RFC&nbsp;2606), which makes
the discovery call fail fast with a `ResourceAccessException` that the post-processor
already catches and logs a warning for. The standard Spring Boot OAuth2 client
auto-configuration is also excluded, since it would otherwise attempt its own
issuer-based discovery instead of using `TestOAuth2Config`'s hand-built registration:

```java
@WebMvcTest(
        controllers = SomeController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
```
