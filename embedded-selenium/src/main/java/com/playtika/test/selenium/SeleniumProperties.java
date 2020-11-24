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
package com.playtika.test.selenium;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.selenium")
public class SeleniumProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_SELENIUM = "embeddedSelenium";
    public static final String BEAN_NAME_EMBEDDED_SELENIUM_DRIVER = "embeddedSeleniumDriver";


    private String imageName;
    private BrowserType browser = BrowserType.CHROMIUM;
    private List<String> arguments = new ArrayList<>();

    private Vnc vnc = new Vnc();

    public FirefoxOptions apply(FirefoxOptions options) {
        options.addArguments(arguments);
        return options;
    }

    public ChromeOptions apply(ChromeOptions options) {
        options.addArguments(arguments);
        return options;
    }

    @Data
    public static class Vnc {
        private File recordingDir = null;
        private VncRecordingMode mode = VncRecordingMode.SKIP;
    }
}