package io.github.danjos.mybankapp.frontui;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class FrontUiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FrontUiApplication.class, args);
    }

}
