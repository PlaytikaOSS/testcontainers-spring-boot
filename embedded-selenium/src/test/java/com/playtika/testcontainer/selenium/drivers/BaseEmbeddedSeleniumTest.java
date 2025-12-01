package com.playtika.testcontainer.selenium.drivers;

import com.playtika.testcontainer.selenium.DockerHostname;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.testcontainers.selenium.BrowserWebDriverContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public abstract class BaseEmbeddedSeleniumTest {

    @Autowired
    protected BrowserWebDriverContainer container;

    @Value("${embedded.selenium.port}")
    protected String seleniumPort;

    @Value("${embedded.selenium.host}")
    protected String seleniumHost;

    @Value("${embedded.selenium.vnc.host}")
    protected String seleniumVncHost;

    @Value("${embedded.selenium.vnc.port}")
    protected String seleniumVncPort;

    @Value("${embedded.selenium.vnc.username}")
    protected String seleniumVncUsername;

    @Value("${embedded.selenium.vnc.password}")
    protected String seleniumVncPassword;

    @Autowired
    protected MutableCapabilities capabilities;

    @LocalServerPort
    private int port;

    @DockerHostname
    private String dockerHostname;

    @Test
    public void seleniumShouldWork() {
        RemoteWebDriver driver = createWebDriver();
        getIndexPage(driver);
        assertThat(driver.getTitle()).isEqualTo("Hello World Page");
    }

    @Test
    public void seleniumLinkShouldWorkAndPropertiesAreAvailable() {
        RemoteWebDriver driver = createWebDriver();
        getIndexPage(driver);
        driver.findElement(By.linkText("Test Link")).click();
        assertThat(driver.getTitle()).isEqualTo("Test Link Page");

        assertThat(seleniumPort).isNotEmpty();
        assertThat(seleniumHost).isNotEmpty();

        assertThat(seleniumVncHost).isNotEmpty();
        assertThat(seleniumVncPort).isNotEmpty();
        assertThat(seleniumVncUsername).isNotEmpty();
        assertThat(seleniumVncPassword).isNotEmpty();
    }

    private void getIndexPage(RemoteWebDriver driver) {
        driver.get("http://" + dockerHostname + ":" + port + "/index.html");
    }

    protected RemoteWebDriver createWebDriver() {
        return new RemoteWebDriver(container.getSeleniumAddress(), capabilities);
    }

    public String getBrowserName() {
        RemoteWebDriver driver = createWebDriver();
        try {
            return (String) driver.getCapabilities().getCapability("browserName");
        } finally {
            driver.quit();
        }
    }

}
