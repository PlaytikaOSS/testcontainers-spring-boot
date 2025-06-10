package com.playtika.testcontainer.selenium;

import com.playtika.testcontainer.selenium.drivers.BaseEmbeddedSeleniumTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
        properties = {
                "embedded.selenium.browser=CHROMIUM",
                "embedded.selenium.vnc.mode=SKIP",
                "embedded.selenium.image-name=selenium/standalone-chrome-debug:3.141.59-mercury"
        }
)
public class OtherCompatibleImageTest extends BaseEmbeddedSeleniumTest {
    @Autowired
    public ChromeOptions options;

    @Value("${embedded.selenium.image-name}")
    String seleniumImageName;

    @Test
    public void testThatIsChromium() {
        assertThat(getBrowserName()).isEqualTo("chrome");
    }

    @Test
    public void propertiesAreSet() {
        assertThat(seleniumImageName)
                .isNotEmpty()
                .isEqualTo("selenium/standalone-chrome-debug:3.141.59-mercury");

        assertThat(container.getDockerImageName()).isEqualTo(seleniumImageName);
    }
}
