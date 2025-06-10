package com.playtika.testcontainer.keycloak.vanilla;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.playtika.testcontainer.keycloak"})
public class VanillaTestApplication {

}
