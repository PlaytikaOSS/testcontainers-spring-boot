package com.playtika.testcontainer.couchbase;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.testcontainers.couchbase.CouchbaseService;

import static java.lang.String.format;

/**
 * https://blog.couchbase.com/testing-spring-data-couchbase-applications-with-testcontainers/
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.couchbase")
public class CouchbaseProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_COUCHBASE = "embeddedCouchbase";
    CouchbaseService[] services = new CouchbaseService[]{
            CouchbaseService.INDEX
            , CouchbaseService.KV
            , CouchbaseService.QUERY
            , CouchbaseService.SEARCH};

    int bucketRamMb = 100;
    String bucketType = "couchbase";

    String host = "localhost";
    String user = "Administrator";
    String password = "password";
    String bucket = "test";

    public void setPassword(String password) {
        if (password.length() < 6) {
            throw new IllegalArgumentException("Couchbase requires password length >= 6 chars, password=" + password);
        }
        this.password = password;
    }

    public String getCredentials() {
        return format("%s:%s", user, password);
    }

    // https://hub.docker.com/_/couchbase   "couchbase:community-7.0.0"
    // https://hub.docker.com/r/couchbase/server
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "couchbase/server:7.2.2";
    }
}
