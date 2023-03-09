package com.playtika.test.mockserver;

import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@SpringBootTest(
        classes = BaseMockServerTest.TestConfiguration.class
)
public abstract class BaseMockServerTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Bean(destroyMethod = "close")
        public MockServerClient mockServerClient(@Value("${embedded.mockserver.host}") String host,
                                                 @Value("${embedded.mockserver.port}") int port) {

            return new MockServerClient(host, port);
        }
    }
}
