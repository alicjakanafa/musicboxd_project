package com.example.MusicBoxd.Config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link WebConfig} wires up the resource handler that serves user-uploaded
 * files, without needing a full Spring context. See {@code AuthControllerTest} for an
 * end-to-end check (through {@code MockMvc}) that a file placed under {@code uploads/} is
 * actually served at {@code /uploads/**}.
 */
class WebConfigTest {

    @Test
    void registersUploadsResourceHandlerBackedByTheUploadsDirectory() throws Exception {
        StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
        MockServletContext servletContext = new MockServletContext();
        applicationContext.setServletContext(servletContext);
        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);

        new WebConfig().addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/**")).isTrue();

        ResourceHttpRequestHandler handler = requestHandlerFor(registry, "/uploads/**");
        assertThat(handler.getLocations())
                .as("the handler must resolve to the project's uploads/ directory on disk")
                .hasSize(1)
                .allSatisfy(resource -> assertThat(resource.getDescription()).contains("uploads"));
    }

    /**
     * {@code ResourceHandlerRegistry} only exposes registered patterns publicly via
     * {@code hasMappingForPattern}; the underlying handler (and therefore the configured
     * resource location) is only reachable through the package-visible registration list.
     * Reflection is used here purely to assert on that already-public, already-serialized
     * configuration value rather than to reach into private implementation state.
     */
    private ResourceHttpRequestHandler requestHandlerFor(
            ResourceHandlerRegistry registry,
            String pattern
    ) throws Exception {
        Method registrationsGetter = ResourceHandlerRegistry.class.getDeclaredMethod("getHandlerMapping");
        registrationsGetter.setAccessible(true);
        Object handlerMapping = registrationsGetter.invoke(registry);
        Method urlMapGetter = handlerMapping.getClass().getMethod("getUrlMap");
        @SuppressWarnings("unchecked")
        var urlMap = (java.util.Map<String, Object>) urlMapGetter.invoke(handlerMapping);
        return (ResourceHttpRequestHandler) urlMap.get(pattern);
    }
}
