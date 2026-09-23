package com.example.MusicBoxd.support;

import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

/**
 * Shared helper for authenticating {@code MockMvc} requests as a signed-in OIDC user.
 *
 * <p>Every controller in this application that needs the current user reads it from the
 * {@link org.springframework.security.oauth2.core.oidc.user.OidcUser} principal placed in the
 * {@code SecurityContext} by Okta's OIDC login flow, keying lookups off the {@code sub} claim
 * (the Okta user id) and reading {@code email}/{@code picture} for profile data. Use these
 * factory methods instead of hand-rolling {@code oidcLogin()} calls so every controller test
 * builds an equivalent principal.
 *
 * <p>Usage:
 * <pre>{@code
 * mockMvc.perform(get("/some/path").with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
 * }</pre>
 */
public final class OidcTestUsers {

    private OidcTestUsers() {
    }

    /**
     * Builds an authenticated OIDC principal with the given {@code sub} (Okta user id) and
     * {@code email} claims. The registration id matches {@link TestOAuth2Config#REGISTRATION_ID}
     * so it lines up with the {@link TestOAuth2Config} client registration imported alongside it.
     */
    public static OidcLoginRequestPostProcessor oidcUser(String sub, String email) {
        return oidcUser(sub, email, null);
    }

    /**
     * Same as {@link #oidcUser(String, String)} but also sets the {@code picture} claim, for
     * tests that assert on the profile picture URL propagated to a newly created {@code User}.
     */
    public static OidcLoginRequestPostProcessor oidcUser(String sub, String email, String picture) {
        return SecurityMockMvcRequestPostProcessors.oidcLogin()
                .clientRegistration(TestOAuth2Config.testClientRegistration())
                .idToken(token -> {
                    token.claim(IdTokenClaimNames.SUB, sub);
                    token.claim("email", email);
                    if (picture != null) {
                        token.claim("picture", picture);
                    }
                });
    }
}
