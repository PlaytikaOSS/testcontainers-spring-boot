package com.playtika.testcontainer.solr;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@SpringBootTest(
        classes = BaseSolrTest.TestConfiguration.class
)
public abstract class BaseSolrTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Bean(destroyMethod = "close")
        public SolrClient solrClient(@Value("${embedded.solr.host}") String host,
                                     @Value("${embedded.solr.port}") int port) {
            return new HttpSolrClient.Builder("http://" + host + ":" + port + "/solr")
                    .build();
        }
    }
}
