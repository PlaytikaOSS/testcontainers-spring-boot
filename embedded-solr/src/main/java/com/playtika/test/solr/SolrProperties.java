package com.playtika.test.solr;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.solr")
public class SolrProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_SOLR = "embeddedSolr";

    int port = 8983;

    @Override
    public String getDefaultDockerImage() {
        return "solr:9.1";
    }
}
