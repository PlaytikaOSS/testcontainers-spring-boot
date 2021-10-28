package com.playtika.test.common.utils;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;


@UtilityClass
public class FileUtils {

    public static String resolveTemplateAsString(ResourceLoader resourceLoader, String fileName, UnaryOperator<String> modifyFunc) {
        String fileTemplateContent = getFileContent(resourceLoader, fileName + ".template");
        return modifyFunc.apply(fileTemplateContent);
    }

    public static Path resolveTemplateAsPath(ResourceLoader resourceLoader, String fileName, UnaryOperator<String> modifyFunc) throws IOException {
        String modifiedFile = resolveTemplateAsString(resourceLoader, fileName, modifyFunc);
        Path tempFilePath = Files.createTempFile("tc_", "_" + fileName);
        tempFilePath.toFile().deleteOnExit();
        Files.write(tempFilePath, modifiedFile.getBytes(StandardCharsets.UTF_8));
        return tempFilePath;
    }

    public static String getFileContent(ResourceLoader resourceLoader, String fileName) {
        Resource resource = resourceLoader.getResource(fileName);
        try (InputStream inputStream = resource.getInputStream()) {
            return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot read resource: %s", resource.getDescription()), e);
        }
    }
}
