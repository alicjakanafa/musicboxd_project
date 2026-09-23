package com.example.MusicBoxd.Config;

import com.example.MusicBoxd.Controller.AuthController;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@link SecurityConfig}'s authorization rules, OAuth2 login success redirect and
 * logout redirect, independently of any single controller's own behaviour.
 *
 * <p>{@code @WebMvcTest} needs at least one controller to stand up a minimal MVC slice;
 * {@link AuthController} is used purely as that vehicle because it has no collaborators of its
 * own, not because these tests are about its behaviour (see {@code AuthControllerTest} for that).
 */
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void permitsUnauthenticatedAccessToStaticResourcePaths() throws Exception {
        // No handler/resource is registered for these in this slice, so a permitted request
        // reaches the (empty) DispatcherServlet handling and results in 404 - the key point is
        // that it is NOT redirected to the OAuth2 login flow like a protected path would be.
        mockMvc.perform(get("/css/site.css"))
                .andExpect(status().isNotFound());
    }

    @Test
    void permitsUnauthenticatedAccessToSpotifyLoginAndCallbackPaths() throws Exception {
        mockMvc.perform(get("/spotify/login"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/spotify/callback"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectsUnauthenticatedRequestsForProtectedPathsToOAuth2Login() throws Exception {
        // "/register" (rather than "/login") is used here as the example protected path:
        // Spring Security's oauth2Login() configuration also installs its own
        // DefaultLoginPageGeneratingFilter which intercepts GET /login unconditionally (see
        // AuthControllerTest for a dedicated test documenting that interaction).
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)
                ));
    }

    @Test
    void allowsAuthenticatedRequestsForProtectedPaths() throws Exception {
        mockMvc.perform(get("/register").with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRedirectsToTheOktaLogoutEndpointWithClientIdAndReturnTo() throws Exception {
        mockMvc.perform(get("/logout").with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "https://test-issuer.invalid/v2/logout?client_id=test-client-id&returnTo=http://localhost"
                ));
    }
}
