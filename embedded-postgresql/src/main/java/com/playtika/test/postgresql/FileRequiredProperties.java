/**
 * 
 */
package com.playtika.test.postgresql;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * @author Wahid Anwar
 * @since 2018-06-01
 */
@Data
@ConfigurationProperties("requeredfiles")
public class FileRequiredProperties {
	private List<FileDetails> pathForFiles;
}
