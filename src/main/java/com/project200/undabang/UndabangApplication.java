package com.project200.undabang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class UndabangApplication {

    public static void main(String[] args) {
        SpringApplication.run(UndabangApplication.class, args);
    }
}
