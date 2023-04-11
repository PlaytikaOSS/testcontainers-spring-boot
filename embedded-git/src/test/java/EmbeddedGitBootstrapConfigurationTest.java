import config.CustomTransportConfigCallback;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.RefSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.Resource;
import util.EncryptionUtils;

import java.io.File;
import java.io.PrintWriter;
import java.security.KeyPair;
import java.security.Security;

import static java.io.File.separator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.org.apache.commons.io.FileUtils.contentEquals;

@Slf4j
@SpringBootTest(
        classes = EmbeddedGitBootstrapConfigurationTest.TestConfiguration.class,
        properties = {
                "spring.profiles.active=enabled",
                "embedded.git.enabled=true",
                "embedded.git.path-to-repositories=src/test/resources/repository",
                "embedded.git.path-to-authorized-keys=src/test/resources/key/embedded-git.pub"
        }
)
class EmbeddedGitBootstrapConfigurationTest {
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Value("classpath:key/pk_pem_encoded.txt")
    Resource pkPemKeyEncoded;
    private static final String REPO_URL_TEMPLATE = "ssh://git@localhost:%s/projects/empty-repository.git";

    @BeforeAll
    public static void beforeAll() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.git.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.git.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.git.password")).isEqualTo("embedded-git-password");
        assertThat(environment.getProperty("embedded.git.path-to-repositories")).isEqualTo("src/test/resources/repository");
    }

    @Test
    @SneakyThrows
    public void testPushViaSsh() {
        KeyPair keyPair = EncryptionUtils.extractKeyPair(pkPemKeyEncoded.getFile(), "embedded-git-passphrase");
        String link = REPO_URL_TEMPLATE.formatted(environment.getProperty("embedded.git.port"));
        String ms = String.valueOf(System.currentTimeMillis());
        String beforeRepoFolderName = "target/before" + ms;

        Git git = Git.cloneRepository()
                .setURI(link)
                .setDirectory(new File(beforeRepoFolderName))
                .setBranch("master")
                .setTransportConfigCallback(new CustomTransportConfigCallback(keyPair))
                .call();
        git.checkout()
                .setCreateBranch(true)
                .setName(beforeRepoFolderName)
                .call();

        String fullFilePath = beforeRepoFolderName + separator + "test_file.txt";
        try (PrintWriter writer = new PrintWriter(fullFilePath, UTF_8)) {
            writer.print("hello world!");
        }
        git.add().addFilepattern("test_file.txt").call();
        git.commit().setMessage("Test commit").call();
        git.push()
                .setRemote("origin")
                .setRefSpecs(new RefSpec(beforeRepoFolderName))
                .setTransportConfigCallback(new CustomTransportConfigCallback(keyPair))
                .call();
        git.close();

        String afterRepoFolderName = "target/after" + ms;

        git = Git.cloneRepository()
                .setURI(link)
                .setDirectory(new File(afterRepoFolderName))
                .setBranch(beforeRepoFolderName)
                .setTransportConfigCallback(new CustomTransportConfigCallback(keyPair))
                .call();
        git.close();

        File pushedFile = new File(fullFilePath);
        File pulledFile = new File(afterRepoFolderName + separator + "test_file.txt");
        assertTrue(contentEquals(pushedFile, pulledFile));
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
