package io.github.danjos.mybankapp;

import io.github.danjos.mybankapp.accounts.AccountsServiceApplication;
import org.springframework.boot.SpringApplication;

public class TestMyBankAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(AccountsServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
