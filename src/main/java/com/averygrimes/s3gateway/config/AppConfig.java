package com.averygrimes.s3gateway.config;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;

import java.util.Arrays;

/**
 * File created by Avery Grimes-Farrow
 * Created on: 2018-12-14
 * https://github.com/helloavery
 */

@Configuration
@Component
public class AppConfig {

    private static final String API_ROOT = "/rest/*";

    @Bean
    public PropertyPlaceholderConfigurer getPropertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocations(new ClassPathResource("application.properties"));
        ppc.setIgnoreUnresolvablePlaceholders(true);
        return ppc;
    }

    @Bean
    public RestTemplate restTemplate(){
        RestTemplate restTemplate = new RestTemplate(new SimpleClientHttpRequestFactory());
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML));
        restTemplate.getMessageConverters().add(messageConverter);
        return restTemplate;
    }

    @Bean
    @Primary
    public DispatcherServletPath dispatcherServletPath() {
        return () -> "/";
    }

    @Bean
    public DispatcherServlet dispatcherServlet() {
        return new DispatcherServlet();
    }

    @Bean
    public ServletRegistrationBean dispatcherServletRegistration() {
        ServletRegistrationBean registration = new ServletRegistrationBean(
                dispatcherServlet(), API_ROOT);
        registration.setName(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME);
        return registration;
    }
}
