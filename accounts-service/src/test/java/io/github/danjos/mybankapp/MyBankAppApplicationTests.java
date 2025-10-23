package io.github.danjos.mybankapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(classes = io.github.danjos.mybankapp.accounts.AccountsServiceApplication.class)
@ActiveProfiles("test")
class MyBankAppApplicationTests {

    @Test
    void contextLoads() {
    }

}
