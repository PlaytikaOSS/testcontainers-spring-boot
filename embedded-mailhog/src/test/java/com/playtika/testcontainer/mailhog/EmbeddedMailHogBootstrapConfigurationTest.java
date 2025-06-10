package com.playtika.testcontainer.mailhog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "embedded.toxiproxy.proxies.mailhog.enabled=true"
        }
)
class EmbeddedMailHogBootstrapConfigurationTest {

    private static final String MAILHOG_API_MESSAGES = "/api/v2/messages";

    @Autowired
    JavaMailSender mailSender;

    @Value("${embedded.mailhog.host}")
    String mailApiHost;

    @Value("${embedded.mailhog.http-port}")
    String mailhogApiPort;

    @Value("${embedded.mailhog.smtp-port}")
    String mailhogSmtpPort;

    @Value("${embedded.mailhog.smtp.toxiproxy.host}")
    String mailhogSmtpToxiproxyHost;

    @Value("${embedded.mailhog.smtp.toxiproxy.port}")
    String mailhogSmtpToxiproxyPort;

    @Value("${embedded.mailhog.smtp.toxiproxy.proxyName}")
    String mailhogSmtpToxiproxyProxyName;

    @Test
    void propertiesAreAvailable() {
        assertThat(mailhogSmtpPort).isNotEmpty();
        assertThat(mailhogApiPort).isNotEmpty();
        assertThat(mailApiHost).isNotEmpty();

        assertThat(mailhogSmtpToxiproxyHost).isNotEmpty();
        assertThat(mailhogSmtpToxiproxyPort).isNotEmpty();
        assertThat(mailhogSmtpToxiproxyProxyName).isNotEmpty();
    }

    @Test
    void shouldSendEmail() throws Exception {
        var mailMessage = buildMail();

        mailSender.send(mailMessage);

        var mailSubjects = fetchEmailSubjectsFromMailHog();
        assertThat(mailSubjects).contains(mailMessage.getSubject());
    }

    private SimpleMailMessage buildMail() {
        var mailMessage = new SimpleMailMessage();
        mailMessage.setSubject("Subject");
        mailMessage.setFrom("no-reply@testcontainers-spring-boot.com");
        mailMessage.setTo("customer@testcontainers-spring-boot.com");
        mailMessage.setText("Lorem ipsum dolor sit amet");
        return mailMessage;
    }

    private List<String> fetchEmailSubjectsFromMailHog() throws Exception {
        var httpClient = HttpClient.newHttpClient();
        var mailHogUri = new URI(String.format("http://%s:%s%s", mailApiHost, mailhogApiPort, MAILHOG_API_MESSAGES));
        var request = HttpRequest.newBuilder().uri(mailHogUri).build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return JsonPath.parse(response.body()).read("$.items[*].Content.Headers.Subject[*]");
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
