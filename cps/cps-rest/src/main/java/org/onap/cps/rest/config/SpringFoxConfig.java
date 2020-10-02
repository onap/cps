package org.onap.cps.rest.config;


import static springfox.documentation.builders.PathSelectors.regex;

import com.google.common.base.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger configuration.
 */
@Configuration
@EnableSwagger2
public class SpringFoxConfig {

    /**
     * Define api configuration.
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                       .select()
                       .apis(RequestHandlerSelectors.any())
                       .paths(paths())
                       .build();
    }

    private Predicate<String> paths() {
        return regex("/model.*");
    }

}

