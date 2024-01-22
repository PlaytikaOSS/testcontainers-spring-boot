package com.playtika.testcontainer.mailhog;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = TestApplication.class,
        properties = {
                "embedded.toxiproxy.proxies.mailhog.enabled=true"
        }
)
class EmbeddedMailHogBootstrapConfigurationTest {

    private static final String MAILHOG_API_MESSAGES = "/api/v2/messages";

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    JavaMailSender mailSender;

    @Value("${embedded.mailhog.host}")
    String mailApiHost;

    @Value("${embedded.mailhog.http-port}")
    String mailhogApiPort;

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.mailhog.smtp-port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mailhog.http-port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mailhog.host")).isNotEmpty();

        assertThat(environment.getProperty("embedded.mailhog.smtp.toxiproxy.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mailhog.smtp.toxiproxy.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mailhog.smtp.toxiproxy.proxyName")).isNotEmpty();
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

}
