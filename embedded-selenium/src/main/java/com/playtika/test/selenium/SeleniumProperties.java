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