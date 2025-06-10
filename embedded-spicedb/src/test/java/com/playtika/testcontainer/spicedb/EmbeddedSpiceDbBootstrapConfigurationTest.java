package com.playtika.testcontainer.spicedb;

import com.authzed.api.v1.SchemaServiceGrpc;
import com.authzed.api.v1.WriteSchemaRequest;
import com.authzed.api.v1.WriteSchemaResponse;
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

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedSpiceDbBootstrapConfigurationTest extends BaseSpiceDbTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Value("${embedded.spicedb.host}")
    String host;

    @Value("${embedded.spicedb.port}")
    int port;

    @Value("${embedded.spicedb.token}")
    String token;

    @Value("${embedded.spicedb.networkAlias}")
    String networkAlias;

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


        WriteSchemaRequest writeSchemaRequest = WriteSchemaRequest.newBuilder().setSchema(schema).build();

        WriteSchemaResponse writeSchemaResponse = schemaService.writeSchema(writeSchemaRequest);

        assertThat(writeSchemaResponse.getWrittenAt().getToken()).isNotEmpty();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(String.valueOf(port)).isNotEmpty();
        assertThat(host).isNotEmpty();
        assertThat(token).isNotEmpty();
        assertThat(networkAlias).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
