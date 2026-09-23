package com.example.MusicBoxd.Config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestTemplateConfigTest {

    @Test
    void restTemplateBeanProducesAUsableRestTemplateInstance() {
        RestTemplateConfig config = new RestTemplateConfig();

        RestTemplate first = config.restTemplate();
        RestTemplate second = config.restTemplate();

        assertThat(first).isNotNull().isInstanceOf(RestTemplate.class);
        assertThat(second)
                .as("the @Bean method itself has no singleton semantics; "
                        + "Spring's container is responsible for that, so each direct "
                        + "invocation must yield a fresh, independently usable instance")
                .isNotNull()
                .isNotSameAs(first);
    }
}
