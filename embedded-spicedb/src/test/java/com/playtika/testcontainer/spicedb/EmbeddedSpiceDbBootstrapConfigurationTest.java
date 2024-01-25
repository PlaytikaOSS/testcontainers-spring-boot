package com.playtika.testcontainer.spicedb;

import com.authzed.api.v1.SchemaServiceGrpc;
import com.authzed.api.v1.SchemaServiceOuterClass;
import com.authzed.grpcutil.BearerToken;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedSpiceDbBootstrapConfigurationTest extends BaseSpiceDbTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;


    @Value("${embedded.spicedb.host}")
    String host;
    @Value("${embedded.spicedb.port}")
    int port;
    @Value("${embedded.spicedb.token}")
    String token;

    @Test
    void shouldConnect() throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(host, port)
                .usePlaintext()
                .build();

        SchemaServiceGrpc.SchemaServiceBlockingStub schemaService = SchemaServiceGrpc.newBlockingStub(channel)
                .withCallCredentials(new BearerToken(token));

        String schema = """
                definition blog/user {}

                definition blog/post {
                    relation reader: blog/user
                    relation writer: blog/user

                    permission read = reader + writer
                    permission write = writer
                }
                """;

        SchemaServiceOuterClass.WriteSchemaRequest request = SchemaServiceOuterClass.WriteSchemaRequest
                .newBuilder()
                .setSchema(schema)
                .build();

        SchemaServiceOuterClass.WriteSchemaResponse response = schemaService.writeSchema(request);

        assertThat(response.getWrittenAt().getToken()).isNotEmpty();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.spicedb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.spicedb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.spicedb.token")).isNotEmpty();
        assertThat(environment.getProperty("embedded.spicedb.networkAlias")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
