package com.example.MusicBoxd.Config;

import me.paulschwarz.springdotenv.DotenvPropertySource;
import org.springframework.boot.bootstrap.ConfigurableBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * spring-dotenv's own listener has no explicit order, so it runs after
 * Spring Boot's EventPublishingRunListener (order 0), which is what fires
 * the event that triggers EnvironmentPostProcessors such as Okta's
 * OktaOAuth2PropertiesMappingEnvironmentPostProcessor. That processor reads
 * AUTH0_ISSUER etc. before .env has been loaded, so it fails to resolve them.
 * Loading .env here, with the highest precedence, guarantees it's in the
 * Environment before that event fires.
 */
public class EarlyDotenvRunListener implements SpringApplicationRunListener, Ordered {

    public EarlyDotenvRunListener(SpringApplication application, String[] args) {
    }

    @Override
    public void environmentPrepared(ConfigurableBootstrapContext bootstrapContext, ConfigurableEnvironment environment) {
        DotenvPropertySource.addToEnvironment(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
