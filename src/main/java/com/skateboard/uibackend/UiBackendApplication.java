package com.skateboard.uibackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class UiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(UiBackendApplication.class, args);
    }
}
