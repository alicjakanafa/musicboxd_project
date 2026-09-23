package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.User;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        controllers = EditProfileController.class,
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
class EditProfileControllerTest {

    private static final Path UPLOAD_DIR = Paths.get("uploads/profile-pictures");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private Set<Path> uploadDirSnapshot() throws IOException {
        if (!Files.isDirectory(UPLOAD_DIR)) {
            return Set.of();
        }
        try (Stream<Path> stream = Files.list(UPLOAD_DIR)) {
            return stream.collect(Collectors.toSet());
        }
    }

    @AfterEach
    void cleanUpAnyFilesCreatedByTests() throws IOException {
        if (!Files.isDirectory(UPLOAD_DIR)) {
            return;
        }
        try (Stream<Path> stream = Files.list(UPLOAD_DIR)) {
            stream.filter(p -> p.getFileName().toString().startsWith("test-upload-marker-"))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private User existingUser() {
        User user = new User("okta-1", "olduser", "old@example.com", "old bio", "");
        user.setId(5L);
        return user;
    }

    @Test
    void getEditProfileUnauthenticatedRedirectsToOAuth2Login() throws Exception {
        mockMvc.perform(get("/profile/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)));
    }

    @Test
    void getEditProfileRedirectsHomeWhenAuthenticatedUserHasNoMatchingRecord() throws Exception {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/profile/edit").with(OidcTestUsers.oidcUser("okta-missing", "missing@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    void getEditProfileRendersFormWithCurrentUserForAuthenticatedOwner() throws Exception {
        User user = existingUser();
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/profile/edit").with(OidcTestUsers.oidcUser("okta-1", "old@example.com")))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-profile"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void postUpdateProfileUnauthenticatedRedirectsToOAuth2Login() throws Exception {
        mockMvc.perform(multipart("/profile/edit")
                        .param("username", "newname")
                        .param("bio", "new bio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID)));

        verify(userRepository, never()).save(any());
    }

    @Test
    void postUpdateProfileRedirectsHomeWhenAuthenticatedUserHasNoMatchingRecord() throws Exception {
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        mockMvc.perform(multipart("/profile/edit")
                        .param("username", "newname")
                        .param("bio", "new bio")
                        .with(OidcTestUsers.oidcUser("okta-missing", "missing@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void postUpdateProfileTrimsAndSavesUsernameAndBioThenRedirectsToProfile() throws Exception {
        User user = existingUser();
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        mockMvc.perform(multipart("/profile/edit")
                        .param("username", "  newname  ")
                        .param("bio", "  new bio  ")
                        .with(OidcTestUsers.oidcUser("okta-1", "old@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/5"));

        verify(userRepository).save(argThat(saved ->
                "newname".equals(saved.getUsername()) && "new bio".equals(saved.getBio())
        ));
    }

    @Test
    void postUpdateProfileLeavesUsernameUnchangedWhenBlank() throws Exception {
        User user = existingUser();
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        mockMvc.perform(multipart("/profile/edit")
                        .param("username", "   ")
                        .param("bio", "updated bio")
                        .with(OidcTestUsers.oidcUser("okta-1", "old@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/5"));

        verify(userRepository).save(argThat(saved ->
                "olduser".equals(saved.getUsername()) && "updated bio".equals(saved.getBio())
        ));
    }

    @Test
    void postUpdateProfileWithProfilePictureUploadsFileAndSavesUrl() throws Exception {
        User user = existingUser();
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        Set<Path> before = uploadDirSnapshot();

        MockMultipartFile file = new MockMultipartFile(
                "profilePicture",
                "test-upload-marker-avatar.png",
                "image/png",
                "fake-image-bytes".getBytes()
        );

        mockMvc.perform(multipart("/profile/edit")
                        .file(file)
                        .param("username", "newname")
                        .param("bio", "bio")
                        .with(OidcTestUsers.oidcUser("okta-1", "old@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/profile/5"));

        verify(userRepository).save(argThat(saved ->
                saved.getProfilePictureUrl() != null
                        && saved.getProfilePictureUrl().startsWith("/uploads/profile-pictures/")
                        && saved.getProfilePictureUrl().endsWith(".png")
        ));

        Set<Path> after = uploadDirSnapshot();
        after.removeAll(before);
        for (Path newFile : after) {
            Files.deleteIfExists(newFile);
        }
    }

    @Test
    void postUpdateProfileWithEmptyProfilePictureDoesNotChangePictureUrl() throws Exception {
        User user = existingUser();
        user.setProfilePictureUrl("/uploads/profile-pictures/existing.png");
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        MockMultipartFile emptyFile = new MockMultipartFile(
                "profilePicture", "empty.png", "image/png", new byte[0]
        );

        mockMvc.perform(multipart("/profile/edit")
                        .file(emptyFile)
                        .param("username", "newname")
                        .param("bio", "bio")
                        .with(OidcTestUsers.oidcUser("okta-1", "old@example.com")))
                .andExpect(status().is3xxRedirection());

        verify(userRepository).save(argThat(saved ->
                "/uploads/profile-pictures/existing.png".equals(saved.getProfilePictureUrl())
        ));
    }

    /**
     * MockMvc's multipart() builder only accepts {@link MockMultipartFile}, so the IOException
     * branch inside {@code updateProfile} (triggered when reading the uploaded file's input
     * stream fails) is exercised here by invoking the controller method directly with a
     * hand-rolled {@link MultipartFile} whose {@code getInputStream()} throws.
     */
    @Test
    void postUpdateProfileSwallowsIOExceptionFromFileUploadAndStillSavesUser() throws Exception {
        User user = existingUser();
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(user));

        org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser =
                org.mockito.Mockito.mock(org.springframework.security.oauth2.core.oidc.user.OidcUser.class);
        when(oidcUser.getSubject()).thenReturn("okta-1");

        org.springframework.security.core.Authentication authentication =
                org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        org.mockito.Mockito.doReturn(oidcUser).when(authentication).getPrincipal();

        MultipartFile brokenFile = new MultipartFile() {
            @Override
            public String getName() {
                return "profilePicture";
            }

            @Override
            public String getOriginalFilename() {
                return "broken.png";
            }

            @Override
            public String getContentType() {
                return "image/png";
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getSize() {
                return 1;
            }

            @Override
            public byte[] getBytes() {
                return new byte[]{1};
            }

            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("disk failure");
            }

            @Override
            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                throw new IOException("disk failure");
            }
        };

        EditProfileController controller = new EditProfileController(userRepository);

        String outcome = controller.updateProfile(authentication, "newname", "bio", brokenFile);

        org.junit.jupiter.api.Assertions.assertEquals("redirect:/profile/5", outcome);
        verify(userRepository).save(argThat(saved ->
                "newname".equals(saved.getUsername()) && "".equals(saved.getProfilePictureUrl())
        ));
    }
}
