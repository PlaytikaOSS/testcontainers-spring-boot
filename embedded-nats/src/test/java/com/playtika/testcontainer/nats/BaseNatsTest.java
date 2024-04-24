package com.playtika.testcontainer.nats;

import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

@Slf4j
@SpringBootTest(
        classes = BaseNatsTest.TestConfiguration.class
)
public abstract class BaseNatsTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Primary
        @Bean(destroyMethod = "close")
        public Connection natsConnection(@Value("${embedded.nats.host}") String host,
                                         @Value("${embedded.nats.port}") int port) throws IOException, InterruptedException {

            Options connectionOptions = getConnectionOptions(host, port, 100);
            return Nats.connect(connectionOptions);
        }

        @Bean(destroyMethod = "close")
        @ConditionalOnToxiProxyEnabled
        public Connection natsToxicproxyConnection(@Value("${embedded.nats.toxiproxy.host}") String host,
                                                   @Value("${embedded.nats.toxiproxy.port}") int port) throws IOException, InterruptedException {
            Options connectionOptions = getConnectionOptions(host, port, 1000);
            return Nats.connect(connectionOptions);
        }
    }

    private static Options getConnectionOptions(String host, int port, int timeoutInMillis) {
        return new Options.Builder()
                .server(String.format("nats://%s:%s", host, port))
                .build();
    }
}
