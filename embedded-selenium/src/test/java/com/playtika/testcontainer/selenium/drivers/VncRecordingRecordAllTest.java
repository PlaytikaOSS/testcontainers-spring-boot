package com.playtika.testcontainer.selenium.drivers;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Test
    public void testThatIsChromium() {
        assertThat(getBrowserName()).isEqualTo("chrome");
    }

    @Test
    public void propertiesAreSet() {
        assertThat(environment.getProperty("embedded.selenium.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.host")).isNotEmpty();

        assertThat(environment.getProperty("embedded.selenium.vnc.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.username")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.password")).isNotEmpty();
        assertThat(environment.getProperty("embedded.selenium.vnc.mode")).isEqualTo("RECORD_ALL");
        assertThat(environment.getProperty("embedded.selenium.vnc.recording-dir")).isNotEmpty();
        File recordDir = new File(environment.getProperty("embedded.selenium.vnc.recording-dir"));
        assertThat(recordDir).exists();
    }
}
