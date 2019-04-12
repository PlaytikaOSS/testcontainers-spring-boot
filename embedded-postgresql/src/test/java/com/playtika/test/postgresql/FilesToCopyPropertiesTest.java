package com.playtika.test.postgresql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.GenericContainer;

import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.playtika.test.postgresql.FilesToCopyProperties.FileDetails;

/**
 * @author Wahid Anwar
 * @since 2018-06-04
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("enabled")
@TestPropertySource(locations = "classpath:files-to-copy.properties")
@SpringBootTest(classes = {FilesToCopyProperties.class})
public class FilesToCopyPropertiesTest {

	@Autowired
	private ConfigurableEnvironment environment;

	@Autowired
	private GenericContainer postgreSQL;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Value("${embedded.postgresql.filestocopy[0].inputResource}")
	private String inputResource;

	@Value("${embedded.postgresql.filestocopy[0].containerPath}")
	private String containerPath;

	@Test
	public void testPropertiesAvailable() {
		assertThat(environment).isNotNull();
		assertThat("classpath:files-to-copy.properties".equals(inputResource)).isTrue();
		assertThat("/".equals(containerPath)).isTrue();
	}

	@Test
	public void testCopyClassPathResourceSuccess() throws IOException {
		FileDetails fromClassPath = new FileDetails();
		fromClassPath.setInputResource(inputResource.replaceFirst("classpath:", ""));
		fromClassPath.setContainerPath(containerPath);
		String containerFileDetails = getContainerFile(fromClassPath);

		List<Container> containers = postgreSQL.getDockerClient().listContainersCmd().exec();
		assertThat(containers).isNotEmpty();
		String containerId = containers.stream().filter(container -> container.getCommand().contains("postgres"))
				.findFirst().map(Container::getId).orElse(null);

		assertThat(containerId).isNotBlank();
		// Checking that the file was copied to the container
		postgreSQL.getDockerClient().copyArchiveFromContainerCmd(containerId, containerFileDetails).exec();
	}

	@Test
	public void testCopyClassPathResourceFailure() throws IOException {
		List<Container> containers = postgreSQL.getDockerClient().listContainersCmd().exec();
		assertThat(containers).isNotEmpty();
		String containerId = containers.stream().filter(container -> container.getCommand().contains("postgres"))
				.findFirst().map(Container::getId).orElse(null);
		// Trying a wrong file, which will throw NotFoundException
		exception.expect(NotFoundException.class);
		postgreSQL.getDockerClient().copyArchiveFromContainerCmd(containerId, "wrongfilename").exec();
	}

	private String getContainerFile(FileDetails details) {
		return String.format("%s%s", details.getContainerPath(), getFileName(details));
	}

	private String getFileName(FileDetails details) {
		return details.getInputResource().substring(
				details.getInputResource().lastIndexOf("/") >= 0 ? details.getInputResource().lastIndexOf("/") : 0,
				details.getInputResource().length());
	}
}
