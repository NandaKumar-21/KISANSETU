package com.kisansetu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KisanSetuApplication {

    public static void main(String[] args) {
        SpringApplication.run(KisanSetuApplication.class, args);
    }
}