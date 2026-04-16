package com.project.iac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IacApplication {

    public static void main(String[] args) {
        SpringApplication.run(IacApplication.class, args);
    }
}
