package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Config.SecurityConfig;
import com.example.MusicBoxd.Model.List;
import com.example.MusicBoxd.Model.ListType;
import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.ListRepository;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ListRepository listRepository;

    @Test
    void redirectsUnauthenticatedUserToOAuth2LoginInsteadOfProcessingAfterLogin() throws Exception {
        mockMvc.perform(get("/users/after-login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(
                        "Location",
                        org.hamcrest.Matchers.startsWith(
                                "/oauth2/authorization/" + TestOAuth2Config.REGISTRATION_ID
                        )
                ));

        verify(userRepository, never()).save(any());
        verify(listRepository, never()).save(any());
    }

    @Test
    void redirectsToHomeAndDoesNotRecreateAnAlreadyExistingUser() throws Exception {
        User existing = new User("okta-1", "existing", "existing@example.com", "", "http://pic");
        existing.setId(7L);
        when(userRepository.findByOktaUserId("okta-1")).thenReturn(Optional.of(existing));

        mockMvc.perform(get("/users/after-login")
                        .with(OidcTestUsers.oidcUser("okta-1", "existing@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(userRepository, never()).save(any());
        verify(listRepository, never()).save(any());
    }

    @Test
    void createsUserAndDefaultListsWhenNoMatchingUserExistsYet() throws Exception {
        when(userRepository.findByOktaUserId("okta-new")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(99L);
            return user;
        });

        mockMvc.perform(get("/users/after-login")
                        .with(OidcTestUsers.oidcUser("okta-new", "new-user@example.com")))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/"));

        verify(userRepository).save(argThat(user ->
                user.getOktaUserId().equals("okta-new")
                        && user.getEmail().equals("new-user@example.com")
                        && user.getUsername().equals("new-user@example.com")
        ));

        verify(listRepository, times(2)).save(any(List.class));
        verify(listRepository).save(argThat(list ->
                list.getUserId().equals(99L) && list.getListType() == ListType.WANT_TO_LISTEN
        ));
        verify(listRepository).save(argThat(list ->
                list.getUserId().equals(99L) && list.getListType() == ListType.FAVOURITES
        ));
    }
}
