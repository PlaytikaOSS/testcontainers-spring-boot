package com.playtika.testcontainer.selenium.drivers;

import com.playtika.testcontainer.selenium.DockerHostname;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.ConfigurableEnvironment;
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

    @Autowired
    protected MutableCapabilities capabilities;

    @Autowired
    protected ConfigurableEnvironment environment;

    @LocalServerPort
    private int port;

    @DockerHostname
    private String dockerHostname;

    private RemoteWebDriver driver;

    protected RemoteWebDriver getDriver() {
        if (driver == null) {
            driver = new RemoteWebDriver(container.getSeleniumAddress(), capabilities);
        }
        return driver;
    }

    @Test
    public void seleniumShouldWork() {
        RemoteWebDriver driver = getDriver();
        getIndexPage(driver);
        assertThat(driver.getTitle()).isEqualTo("Hello World Page");
    }

    @Test
    public void seleniumLinkShouldWorkAndPropertiesAreAvailable() {
        RemoteWebDriver driver = getDriver();
        getIndexPage(driver);
        driver.findElement(By.linkText("Test Link")).click();
        assertThat(driver.getTitle()).isEqualTo("Test Link Page");

        assertThat(environment.getProperty("embedded.selenium.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.host")).isNotEmpty();

        assertThat(environment.getProperty("embedded.selenium.vnc.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.username")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.password")).isNotEmpty();
    }

    private void getIndexPage(RemoteWebDriver driver) {
        driver.get("http://" + dockerHostname + ":" + port + "/index.html");
    }

    public String getBrowserName() {
        return (String) getDriver().getCapabilities().getCapability("browserName");
    }

}
