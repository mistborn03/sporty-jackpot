package com.sporty.jackpot;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(info = @Info(
        title = "Sporty Jackpot API",
        version = "1.0.0",
        description = "Accepts bets, contributes them to jackpot pools, and evaluates bets for rewards."))
@SpringBootApplication
public class JackpotApplication {

    public static void main(String[] args) {
        SpringApplication.run(JackpotApplication.class, args);
    }
}
