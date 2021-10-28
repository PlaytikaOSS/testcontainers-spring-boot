package com.playtika.test.selenium.drivers;


import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@TestPropertySource(
        properties = {
                "embedded.selenium.browser=CHROMIUM",
                "embedded.selenium.arguments=start-maximized"
        }
)
public class EmbeddedChromiumSeleniumTest extends BaseEmbeddedSeleniumTest {
    @Autowired
    public ChromeOptions options;

    @Test
    public void testThatIsChromium() {
        assertThat(getBrowserName()).isEqualTo("chrome");
    }

    @Test
    public void testThatOptionIsSet() {
        Map<String, Object> capabilities = (Map<String, Object>) options.asMap().get(ChromeOptions.CAPABILITY);
        List<String> args = (List<String>)capabilities.get("args");
        assertThat(args).contains("start-maximized");
    }

    @Test
    public void vncModeIsSkipByDefault() {
        assertThat(environment.getProperty("embedded.selenium.vnc.mode")).isEqualTo("SKIP");
    }
}
