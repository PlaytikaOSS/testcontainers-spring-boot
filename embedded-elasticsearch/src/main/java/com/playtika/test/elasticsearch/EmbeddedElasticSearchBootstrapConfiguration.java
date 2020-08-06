/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.elasticsearch;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;
import static com.playtika.test.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.elasticsearch.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ElasticSearchProperties.class)
public class EmbeddedElasticSearchBootstrapConfiguration {

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH, destroyMethod = "stop")
    public ElasticsearchContainer elasticSearch(ConfigurableEnvironment environment,
                                                ElasticSearchProperties properties) {
        log.info("Starting ElasticSearch server. Docker image: {}", properties.dockerImage);

        ElasticsearchContainer elasticSearch = ElasticSearchContainerFactory.create(properties, log);
        startAndLogTime(elasticSearch);
        registerElasticSearchEnvironment(elasticSearch, environment, properties);
        return elasticSearch;
    }

    private void registerElasticSearchEnvironment(ElasticsearchContainer elasticSearch,
                                                  ConfigurableEnvironment environment,
                                                  ElasticSearchProperties properties) {
        Integer httpPort = elasticSearch.getMappedPort(properties.httpPort);
        Integer transportPort = elasticSearch.getMappedPort(properties.transportPort);
        String host = elasticSearch.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.elasticsearch.clusterName", properties.clusterName);
        map.put("embedded.elasticsearch.host", host);
        map.put("embedded.elasticsearch.httpPort", httpPort);
        map.put("embedded.elasticsearch.transportPort", transportPort);

        log.info("Started ElasticSearch server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedElasticSearchInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
