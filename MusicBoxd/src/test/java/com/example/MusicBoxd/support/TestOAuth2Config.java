package com.example.MusicBoxd.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Provides a single dummy OAuth2/OIDC {@link ClientRegistrationRepository} bean for
 * {@code @WebMvcTest} slices that load {@code SecurityConfig}.
 *
 * <p>{@code SecurityConfig} configures {@code httpSecurity.oauth2Login(...)}, and Spring
 * Security requires a {@link ClientRegistrationRepository} bean to exist in the application
 * context in order to build that part of the filter chain - even when a test never actually
 * exercises the real OAuth2 redirect/token exchange (for example when using
 * {@code SecurityMockMvcRequestPostProcessors.oidcLogin()} to inject an already-authenticated
 * user). None of the values below need to be real; they only need to be well-formed enough for
 * Spring Security to build the registration.
 *
 * <p>Full-context {@code @SpringBootTest} tests (e.g. {@code MusicBoxdApplicationTests}) also
 * import this so the application does not attempt OIDC discovery against the Auth0 issuer at
 * startup.
 *
 * <p>The registration id is {@code "okta"}, matching the single provider this application
 * integrates with.
 */
@TestConfiguration
public class TestOAuth2Config {

    public static final String REGISTRATION_ID = "okta";

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(testClientRegistration());
    }

    public static ClientRegistration testClientRegistration() {
        return ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://test.okta.com/oauth2/v1/authorize")
                .tokenUri("https://test.okta.com/oauth2/v1/token")
                .userInfoUri("https://test.okta.com/oauth2/v1/userinfo")
                .jwkSetUri("https://test.okta.com/oauth2/v1/keys")
                .userNameAttributeName("sub")
                .clientName("Okta")
                .build();
    }
}
