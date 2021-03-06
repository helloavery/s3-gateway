package com.averygrimes.secretschest.config;

import com.averygrimes.cache.EnableCacheService;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

@EnableCacheService
@Configuration
public class AppConfig {

    @Bean
    public PropertyPlaceholderConfigurer getPropertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocations(new ClassPathResource("application.yml"));
        ppc.setIgnoreUnresolvablePlaceholders(true);
        return ppc;
    }
}
