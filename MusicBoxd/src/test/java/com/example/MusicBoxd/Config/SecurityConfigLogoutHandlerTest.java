package com.example.MusicBoxd.Config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Directly exercises the {@code LogoutHandler} built by {@link SecurityConfig#logoutHandler()}.
 * That handler is private and normally only reachable through the full {@code SecurityFilterChain}
 * built from {@code HttpSecurity}, which needs a running Spring Security context; invoking it
 * directly via reflection lets both the happy path and the (otherwise hard to trigger)
 * {@code IOException} branch be verified without spinning up a full application context.
 */
class SecurityConfigLogoutHandlerTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    private LogoutHandler logoutHandler() {
        ReflectionTestUtils.setField(securityConfig, "issuer", "https://test-issuer.invalid/");
        ReflectionTestUtils.setField(securityConfig, "clientId", "test-client-id");
        return (LogoutHandler) ReflectionTestUtils.invokeMethod(securityConfig, "logoutHandler");
    }

    @AfterEach
    void resetRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void redirectsToTheConfiguredIssuersLogoutEndpoint() {
        LogoutHandler handler = logoutHandler();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // ServletUriComponentsBuilder.fromCurrentContextPath() (used inside the handler to build
        // the "returnTo" URL) reads the current request from RequestContextHolder rather than
        // from the request passed into LogoutHandler.logout(...), so it must be bound here to
        // mirror what a real servlet container/DispatcherServlet request does automatically.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        handler.logout(request, response, null);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("https://test-issuer.invalid/v2/logout?client_id=test-client-id&returnTo=http://localhost");
    }

    @Test
    void wrapsAnIOExceptionFromSendRedirectInARuntimeException() throws IOException {
        LogoutHandler handler = logoutHandler();
        MockHttpServletRequest requestForContext = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(requestForContext));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Authentication authentication = mock(Authentication.class);
        doThrow(new IOException("boom")).when(response).sendRedirect(org.mockito.ArgumentMatchers.anyString());

        assertThatThrownBy(() -> handler.logout(request, response, authentication))
                .isInstanceOf(RuntimeException.class)
                .hasCauseInstanceOf(IOException.class);
    }
}
