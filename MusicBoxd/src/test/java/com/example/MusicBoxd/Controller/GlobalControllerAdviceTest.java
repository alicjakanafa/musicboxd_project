package com.example.MusicBoxd.Controller;

import com.example.MusicBoxd.Model.User;
import com.example.MusicBoxd.Repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalControllerAdviceTest {

    @Mock
    private UserRepository userRepository;

    private GlobalControllerAdvice advice;

    @Test
    void returnsNullWhenAuthenticationIsNull() {
        advice = new GlobalControllerAdvice(userRepository);

        User result = advice.currentUser(null);

        assertThat(result).isNull();
        verify(userRepository, never()).findByOktaUserId(any());
    }

    @Test
    void returnsNullWhenAuthenticationIsNotAuthenticated() {
        advice = new GlobalControllerAdvice(userRepository);
        Authentication unauthenticated = new TestingAuthenticationToken("okta-1", "credentials");
        unauthenticated.setAuthenticated(false);

        User result = advice.currentUser(unauthenticated);

        assertThat(result).isNull();
        verify(userRepository, never()).findByOktaUserId(any());
    }

    @Test
    void returnsNullWhenAuthenticatedUserHasNoMatchingRecord() {
        advice = new GlobalControllerAdvice(userRepository);
        Authentication authenticated = new TestingAuthenticationToken("okta-missing", "credentials");
        authenticated.setAuthenticated(true);
        when(userRepository.findByOktaUserId("okta-missing")).thenReturn(Optional.empty());

        User result = advice.currentUser(authenticated);

        assertThat(result).isNull();
    }

    @Test
    void returnsMatchingUserWhenAuthenticatedUserIsFound() {
        advice = new GlobalControllerAdvice(userRepository);
        Authentication authenticated = new TestingAuthenticationToken("okta-42", "credentials");
        authenticated.setAuthenticated(true);
        User existing = new User("okta-42", "listener", "listener@example.com", "bio", "http://pic");
        when(userRepository.findByOktaUserId("okta-42")).thenReturn(Optional.of(existing));

        User result = advice.currentUser(authenticated);

        assertThat(result).isSameAs(existing);
    }
}
