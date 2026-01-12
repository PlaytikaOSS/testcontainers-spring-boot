package com.playtika.testcontainer.selenium.drivers;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
        properties = {
                "embedded.selenium.browser=CHROMIUM",
                "embedded.selenium.vnc.mode=RECORD_ALL"
        }
)
public class VncRecordingRecordAllTest extends BaseEmbeddedSeleniumTest {
    @Autowired
    public ChromeOptions options;

    @Value("${embedded.selenium.vnc.mode}")
    String seleniumVncMode;

    @Value("${embedded.selenium.vnc.recording-dir:}")
    String seleniumVncRecordingDir;

    @Test
    public void testThatIsChromium() {
        assertThat(getBrowserName()).isEqualTo("chrome");
    }

    @Test
    public void propertiesAreSet() {
        assertThat(seleniumPort).isNotEmpty();
        assertThat(seleniumHost).isNotEmpty();

        assertThat(seleniumVncHost).isNotEmpty();
        assertThat(seleniumVncPort).isNotEmpty();
        assertThat(seleniumVncUsername).isNotEmpty();
        assertThat(seleniumVncPassword).isNotEmpty();
        assertThat(seleniumVncMode).isEqualTo("RECORD_ALL");

        // Only check recording directory if it's set
        if (!seleniumVncRecordingDir.isEmpty()) {
            File recordDir = new File(seleniumVncRecordingDir);
            assertThat(recordDir).exists();
        }
    }
}
