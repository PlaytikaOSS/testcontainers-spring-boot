package com.playtika.test.selenium.drivers;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(
    properties = {
        "embedded.selenium.browser=CHROMIUM",
        "embedded.selenium.vnc.mode=RECORD_ALL"
    }
)
@ContextConfiguration(
    initializers = VncRecordingRecordAllUserDefinesDirTest.PropertyOverrideContextInitializer.class,
    classes = TestApplication.class)
@TestInstance(Lifecycle.PER_CLASS)
public class VncRecordingRecordAllUserDefinesDirTest extends BaseEmbeddedSeleniumTest {
    @Autowired
    public ChromeOptions options;

    @Value("${embedded.selenium.vnc.recording-dir}")
    private String recordDir;


    @AfterAll
    public void cleanupTmpDir() {
        File dirToDelete = new File(recordDir);
        String[] tmpFiles = dirToDelete.exists() ? dirToDelete.list() : null;
        if (tmpFiles == null) {
            return;
        }

        FileSystemUtils.deleteRecursively(dirToDelete);

        if (tmpFiles.length == 0) {
            return;
        }

        //assert that all tests generated a video
        assertThat(tmpFiles.length).isGreaterThanOrEqualTo(3);
    }

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
        assertThat(environment.getProperty("embedded.selenium.vnc.wassetintest")).isEqualTo("true");

        assertThat(environment.getProperty("embedded.selenium.vnc.recording-dir")).isNotEmpty();
        assertThat(new File(environment.getProperty("embedded.selenium.vnc.recording-dir"))).exists();
    }

    static class PropertyOverrideContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            Path tmpDir = Files.createTempDirectory("UnitTest");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext, "embedded.selenium.vnc.recording-dir=" + tmpDir.toAbsolutePath());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                configurableApplicationContext, "embedded.selenium.vnc.wassetintest=true");
        }
    }

}
