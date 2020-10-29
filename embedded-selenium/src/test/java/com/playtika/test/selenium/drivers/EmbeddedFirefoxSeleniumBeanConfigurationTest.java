/*
 * The MIT License (MIT)
 *
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
package com.playtika.test.selenium.drivers;


import com.playtika.test.selenium.SeleniumProperties;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
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
            options.setCapability(CapabilityType.ACCEPT_SSL_CERTS, false);
            options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, false);
            return options;
        }
    }
}
