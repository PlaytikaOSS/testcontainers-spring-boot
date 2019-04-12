package com.playtika.test.postgresql;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

/**
 * @author Wahid Anwar
 * @since 2018-06-01
 */
@Data
@ConfigurationProperties("embedded.postgresql")
public class FilesToCopyProperties {
	private List<FileDetails> filestocopy = new ArrayList<>();

	@Data
	public static class FileDetails {
		private String inputResource;
		private String containerPath;
	}
}
