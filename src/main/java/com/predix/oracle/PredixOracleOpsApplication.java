package com.predix.oracle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PredixOracleOpsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PredixOracleOpsApplication.class, args);
    }
}
