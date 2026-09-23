package com.example.MusicBoxd.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * Supplies a static OAuth2 client registration for full-context tests so the
 * application does not attempt OIDC discovery against the Auth0 issuer at startup.
 */
@TestConfiguration
public class TestOAuth2Config {

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("okta")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("https://test.example.com/authorize")
                .tokenUri("https://test.example.com/oauth/token")
                .jwkSetUri("https://test.example.com/.well-known/jwks.json")
                .userInfoUri("https://test.example.com/userinfo")
                .userNameAttributeName("sub")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }
}
