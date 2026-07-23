package com.hendrik.javaledgerapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaLedgerApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaLedgerApiApplication.class, args);
    }

}
