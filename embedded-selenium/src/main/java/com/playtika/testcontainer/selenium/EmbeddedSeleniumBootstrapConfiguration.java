package com.playtika.testcontainer.selenium;

import com.google.common.net.InetAddresses;
import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.DefaultRecordingFileFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RecordingFileFactory;
import org.testcontainers.selenium.BrowserWebDriverContainer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.selenium.SeleniumProperties.BEAN_NAME_EMBEDDED_SELENIUM;


@Slf4j
@AutoConfiguration(after = DockerPresenceBootstrapConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.selenium.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SeleniumProperties.class)
@RequiredArgsConstructor
public class EmbeddedSeleniumBootstrapConfiguration {

    private static final String SELENIUM_NETWORK_ALIAS = "selenium.testcontainer.docker";
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
    public BrowserWebDriverContainer selenium(ConfigurableEnvironment environment,
                                              SeleniumProperties properties,
                                              Optional<Network> network,
                                              ContainerStartupCoordinator startupCoordinator) {

        BrowserWebDriverContainer container = new BrowserWebDriverContainer(ContainerUtils.getDockerImageName(properties))
            .withRecordingFileFactory(getRecordingFileFactory())
            .withNetworkAliases(SELENIUM_NETWORK_ALIAS);
        network.ifPresent(container::withNetwork);
        File recordingDirOrNull = null;
        if (properties.getVnc().getMode().convert() != BrowserWebDriverContainer.VncRecordingMode.SKIP) {
            recordingDirOrNull = getOrCreateTempDir(properties.getVnc().getRecordingDir());
        }
        container.withRecordingMode(properties.getVnc().getMode().convert(), recordingDirOrNull);

        BrowserWebDriverContainer configuredContainer = (BrowserWebDriverContainer) ContainerUtils.configureCommons(container, properties, log);

        File finalRecordingDirOrNull = recordingDirOrNull;
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredContainer, log);
            Map<String, Object> seleniumEnv = registerSeleniumEnvironment(environment, configuredContainer, properties.getVnc().getMode().convert(), finalRecordingDirOrNull);
            log.info("Started Selenium server. Connection details: {}", seleniumEnv);
        });
        return configuredContainer;
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
        map.put("embedded.selenium.networkAlias", SELENIUM_NETWORK_ALIAS);

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
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            return "host.docker.internal";
        } else if (OS.contains("win")) {
            return "host.docker.internal";
        } else if (OS.contains("nux")) {
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

        return DOCKER_FOR_LINUX_STATIC_IP;
    }

    private boolean isValidIpAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        return InetAddresses.isInetAddress(ipAddress);
    }

}
