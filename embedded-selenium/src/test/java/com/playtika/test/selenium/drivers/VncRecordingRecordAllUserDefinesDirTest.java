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

    @Value("${embedded.selenium.vnc.record-dir}")
    private String recordDir;


    @AfterAll
    public void cleanupTmpDir() {
        File dirToDelete = new File(recordDir);
        if (dirToDelete.exists()) {
            FileSystemUtils.deleteRecursively(new File(recordDir));
        }
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

        assertThat(environment.getProperty("embedded.selenium.vnc.record-dir")).isNotEmpty();
        File recordDir = new File(environment.getProperty("embedded.selenium.vnc.record-dir"));
        assertThat(recordDir).exists();
    }

    static class PropertyOverrideContextInitializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @SneakyThrows
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            Path tmpDir = Files.createTempDirectory("UnitTest");
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext, "embedded.selenium.vnc.record-dir=" + tmpDir.toAbsolutePath().toString());
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    configurableApplicationContext, "embedded.selenium.vnc.wassetintest=" + true);

        }
    }

}

