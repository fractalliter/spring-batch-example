package com.ecom.winners;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WinnersApplication {

    public static void main(String[] args) {
        SpringApplication.run(WinnersApplication.class, args);
    }

}
