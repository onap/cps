package org.onap.cps.ncmp.config;

import org.modelmapper.ModelMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestConfig {
    /**
     * ModelMapper configuration.
     */
    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}