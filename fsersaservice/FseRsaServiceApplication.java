package it.thcs.fse.fsersaservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FseRsaServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FseRsaServiceApplication.class, args);
    }
}