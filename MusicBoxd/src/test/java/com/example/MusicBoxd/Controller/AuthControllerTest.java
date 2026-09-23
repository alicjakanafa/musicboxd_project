package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Config.WebConfig;
import com.example.MusicBoxd.Repository.UserRepository;
import com.example.MusicBoxd.support.OidcTestUsers;
import com.example.MusicBoxd.support.TestOAuth2Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = {
                OAuth2ClientAutoConfiguration.class,
                OAuth2ClientWebSecurityAutoConfiguration.class
        }
)
@Import({SecurityConfig.class, WebConfig.class, TestOAuth2Config.class})
@TestPropertySource(properties = {
        "okta.oauth2.issuer=https://test-issuer.invalid/",
        "okta.oauth2.client-id=test-client-id"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private Path uploadedFile;

    @AfterEach
    void cleanUpUploadedFile() throws IOException {
        if (uploadedFile != null) {
            Files.deleteIfExists(uploadedFile);
        }
    }

    /**
     * NOTE - suspected production defect: {@link SecurityConfig} configures
     * {@code oauth2Login(...)} without a custom {@code .loginPage(...)}, so Spring Security
     * installs its own {@code DefaultLoginPageGeneratingFilter}, which unconditionally
     * intercepts every {@code GET /login} request (authenticated or not, see
     * {@code isLoginUrlRequest} in that filter) and renders its own generated HTML page instead
     * of forwarding to a handler. As a result {@link AuthController#login()} - and the
     * {@code login.html} Thymeleaf template it returns - can never actually be reached in this
     * application as currently wired; every request to {@code /login} is answered by Spring
     * Security's generic "Please sign in" page rather than the application's own view. This test
     * documents that actual, currently-observable behaviour rather than the (unreachable)
     * intended one; it should be revisited if {@code SecurityConfig} is ever updated to point
     * {@code oauth2Login().loginPage(...)} at {@code /login} explicitly.
     */
    @Test
    void loginPathIsInterceptedBySpringSecuritysGeneratedPageInsteadOfAuthControllersView() throws Exception {
        mockMvc.perform(get("/login").with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Please sign in")));
    }

    @Test
    void showsRegisterViewForAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/register").with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void redirectsUnauthenticatedUserAwayFromRegister() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().exists("Location"));
    }

    @Test
    void servesAnExistingFileFromTheUploadsDirectoryForAnAuthenticatedUser() throws Exception {
        Path uploadsDir = Path.of("uploads");
        Files.createDirectories(uploadsDir);
        uploadedFile = uploadsDir.resolve("web-config-test-" + System.nanoTime() + ".txt");
        Files.writeString(uploadedFile, "uploaded-file-contents", StandardCharsets.UTF_8);

        mockMvc.perform(get("/uploads/" + uploadedFile.getFileName())
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string("uploaded-file-contents"));
    }

    @Test
    void returnsNotFoundForAMissingFileUnderTheUploadsDirectory() throws Exception {
        mockMvc.perform(get("/uploads/does-not-exist-" + System.nanoTime() + ".txt")
                        .with(OidcTestUsers.oidcUser("okta-1", "user@example.com")))
                .andExpect(status().isNotFound());
    }
}
