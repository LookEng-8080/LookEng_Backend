package com.sw8080.lookeng;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LookEngApplication {

    public static void main(String[] args) {
        SpringApplication.run(LookEngApplication.class, args);
    }

}
