package com.playtika.test.selenium;

import com.google.common.net.InetAddresses;
import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static com.playtika.test.selenium.SeleniumProperties.BEAN_NAME_EMBEDDED_SELENIUM;
import static java.time.temporal.ChronoUnit.SECONDS;


@Slf4j
@AutoConfiguration(after = DockerPresenceBootstrapConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.selenium.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SeleniumProperties.class)
@RequiredArgsConstructor
public class EmbeddedSeleniumBootstrapConfiguration {

    private static final String TC_TEMP_DIR_PREFIX = "tc";
    public static final String DEFINED_VNC_USERNAME = "vnc";
    public static final String DEFINED_VNC_PASSWORD = "secret";

    public static final String DOCKER_FOR_LINUX_STATIC_IP = "172.17.0.1";

    @Bean
    @ConditionalOnMissingBean(MutableCapabilities.class)
    @ConditionalOnProperty(value = "embedded.selenium.browser", havingValue = "CHROMIUM", matchIfMissing = true)
    public ChromeOptions chromeOptions(SeleniumProperties properties) {
        ChromeOptions options = new ChromeOptions();
        properties.apply(options);
        return options;
    }

    @Bean
    @ConditionalOnMissingBean(MutableCapabilities.class)
    @ConditionalOnProperty(value = "embedded.selenium.browser", havingValue = "FIREFOX")
    public FirefoxOptions firefoxOptions(SeleniumProperties properties) {
        FirefoxOptions options = new FirefoxOptions();
        properties.apply(options);
        return options;
    }


    @Bean(name = BEAN_NAME_EMBEDDED_SELENIUM, destroyMethod = "stop")
    @ConditionalOnMissingBean
    public BrowserWebDriverContainer selenium(
            ConfigurableEnvironment environment,
            SeleniumProperties properties,
            MutableCapabilities capabilities,
            @Deprecated @Value("${embedded.selenium.imageName:#{null}}") String deprImageName
    ) {

        if (deprImageName != null) {
            throw new IllegalArgumentException("`embedded.selenium.imageName` property is deprecated. Please replace `embedded.selenium.imageName` property with `embedded.selenium.dockerImage` property.");
        }
        BrowserWebDriverContainer container = isNotBlank(properties.getDockerImage())
                ? new BrowserWebDriverContainer<>(ContainerUtils.getDockerImageName(properties))
                : new BrowserWebDriverContainer<>();

        container.setWaitStrategy(getWaitStrategy());
        container.withCapabilities(capabilities);
        container.withRecordingFileFactory(getRecordingFileFactory());

        File recordingDirOrNull = null;
        if (properties.getVnc().getMode().convert() != BrowserWebDriverContainer.VncRecordingMode.SKIP) {
            recordingDirOrNull = getOrCreateTempDir(properties.getVnc().getRecordingDir());
        }
        container.withRecordingMode(properties.getVnc().getMode().convert(), recordingDirOrNull);

        ContainerUtils.configureCommonsAndStart(container, properties, log);

        Map<String, Object> seleniumEnv = registerSeleniumEnvironment(environment, container, properties.getVnc().getMode().convert(), recordingDirOrNull);
        log.info("Started Selenium server. Connection details: {}", seleniumEnv);
        return container;
    }

    //See: https://github.com/testcontainers/testcontainers-java/pull/4357
    @Deprecated
    private WaitStrategy getWaitStrategy() {
        WaitStrategy logWaitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*(RemoteWebDriver instances should connect to|Selenium Server is up and running).*\n")
                .withStartupTimeout(Duration.of(60, SECONDS));

        return new WaitAllStrategy()
                .withStrategy(logWaitStrategy)
                .withStrategy(new HostPortWaitStrategy())
                .withStartupTimeout(Duration.of(60, SECONDS));
    }

    /**
     * Testcontainers does not expose its default vnc dir when it is not
     * defined, so we recreate this implementation here.
     *
     * @param vncRecordingDirProperty
     * @return
     */
    private File getOrCreateTempDir(File vncRecordingDirProperty) {
        if (vncRecordingDirProperty != null) {
            return vncRecordingDirProperty;
        }

        try {
            return Files.createTempDirectory(TC_TEMP_DIR_PREFIX).toFile();
        } catch (IOException e) {
            // should never happen as per javadoc, since we use valid prefix
            log.error("Exception while trying to create temp directory ", e);
            throw new ContainerLaunchException("Exception while trying to create temp directory", e);
        }
    }

    /**
     * Users can redefine this to create other strategies of saving
     * vnc recordings
     *
     * @return
     */
    @Bean
    @ConditionalOnMissingBean
    public RecordingFileFactory getRecordingFileFactory() {
        return new DefaultRecordingFileFactory();
    }

    private Map<String, Object> registerSeleniumEnvironment(ConfigurableEnvironment environment, BrowserWebDriverContainer container, BrowserWebDriverContainer.VncRecordingMode vncMode, File recordingDirOrNull) {
        URL seleniumAddress = container.getSeleniumAddress();
        String vncAddress = container.getVncAddress();
        URI vncURI = URI.create(vncAddress);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.selenium.port", seleniumAddress.getPort());
        map.put("embedded.selenium.host", seleniumAddress.getHost());
        map.put("embedded.selenium.vnc.port", vncURI.getPort());
        map.put("embedded.selenium.vnc.host", vncURI.getHost());
        map.put("embedded.selenium.vnc.username", DEFINED_VNC_USERNAME);
        map.put("embedded.selenium.vnc.password", DEFINED_VNC_PASSWORD);
        map.put("embedded.selenium.vnc.recording-dir", recordingDirOrNull);
        map.put("embedded.selenium.vnc.mode", vncMode);
        map.put("embedded.selenium.dockerhost", getHostName(container));

        MapPropertySource propertySource = new MapPropertySource("embeddedSeleniumInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        return map;
    }


    /**
     * Implementation partly based upon
     * <p>
     * https://stackoverflow.com/questions/22944631/how-to-get-the-ip-address-of-the-docker-host-from-inside-a-docker-container
     *
     * @param container
     * @return
     */
    public String getHostName(GenericContainer<?> container) {
        // unfortunately host.docker.internal only works for mac and windows :(
        // and we need to work out the hostname for linux.
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
            return "host.docker.internal";
        } else if (OS.indexOf("win") >= 0) {
            return "host.docker.internal";
        } else if (OS.indexOf("nux") >= 0) {
            Container.ExecResult execResult;
            try {
                execResult = container.execInContainer("/sbin/ip route|awk '/default/ { print $3 }'");
            } catch (IOException e) {
                log.warn("Cannot find host ip", e);
                return DOCKER_FOR_LINUX_STATIC_IP;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return DOCKER_FOR_LINUX_STATIC_IP;
            }
            String hostIpAddress = execResult.getStdout();
            if (isValidIpAddress(hostIpAddress)) {
                return hostIpAddress;
            } else {
                return DOCKER_FOR_LINUX_STATIC_IP;
            }
        }

        // currently only supported if docker machine is installed.
        // otherwise throws an UnsupportedOpertaion exception
        return container.getTestHostIpAddress();
    }

    private boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        return InetAddresses.isInetAddress(ipAddress);
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}