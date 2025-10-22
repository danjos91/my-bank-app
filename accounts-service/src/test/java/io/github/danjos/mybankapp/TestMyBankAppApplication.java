package io.github.danjos.mybankapp;

import org.springframework.boot.SpringApplication;

public class TestMyBankAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(MyBankAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
