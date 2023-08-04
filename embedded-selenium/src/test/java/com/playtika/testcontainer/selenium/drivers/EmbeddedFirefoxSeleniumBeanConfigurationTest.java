package com.playtika.testcontainer.selenium.drivers;


import com.playtika.testcontainer.selenium.SeleniumProperties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that user of starter can reconfigure browser using bean configuration
 *
 * {@link FirefoxOptions}
 */
@Disabled
@Import(EmbeddedFirefoxSeleniumBeanConfigurationTest.LocalTestConfiguration.class)
public class EmbeddedFirefoxSeleniumBeanConfigurationTest extends BaseEmbeddedSeleniumTest {

    @Autowired
    public FirefoxOptions options;

    @Test
    public void testThatIsFirefox() {
        assertThat(getBrowserName()).isEqualTo("firefox");
    }

    @Test
    public void testThatTestArgumentIsSet() {
        Map<String, Object> capabilities = (Map<String, Object>) options.asMap().get(FirefoxOptions.FIREFOX_OPTIONS);
        List<String> args = (List<String>)capabilities.get("args");
        assertThat(args).contains("hello-world");

    }

    @Test
    public void testThatTestCapabilityIsSet() {
        // default is true, false is set in localtestconfiguration
        boolean acceptUnsecureCertificates = (boolean)container.getWebDriver().getCapabilities().getCapability(CapabilityType.ACCEPT_INSECURE_CERTS);
        assertThat(acceptUnsecureCertificates).isFalse();
    }

    @TestConfiguration
    static class LocalTestConfiguration {

        @Bean
        public FirefoxOptions firefoxOptions(SeleniumProperties properties) {

            FirefoxOptions options = new FirefoxOptions();
            properties.apply(options);
            options.addArguments("hello-world");
            options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, false);
            options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, false);
            return options;
        }
    }
}
