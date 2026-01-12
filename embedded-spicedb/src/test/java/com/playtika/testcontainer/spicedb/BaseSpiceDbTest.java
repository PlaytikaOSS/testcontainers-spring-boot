package com.playtika.testcontainer.spicedb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;

@Slf4j
@SpringBootTest(
        classes = BaseSpiceDbTest.TestConfiguration.class
)
public abstract class BaseSpiceDbTest {

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

    }
}
