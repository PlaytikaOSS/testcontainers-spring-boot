package com.playtika.test.common.utils;

import lombok.experimental.UtilityClass;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

@UtilityClass
public class FileUtils {

    public static void resolveTemplate(ResourceLoader resourceLoader, String fileName, Function<String, String> modifyFunc) throws Exception {
        String fileTemplateContent = getFileContent(resourceLoader, fileName + ".template");
        String modifiedFile = modifyFunc.apply(fileTemplateContent);
        writeToFileInClassesDir(modifiedFile, fileName);
    }

    public static void writeToFileInClassesDir(String body, String fileName) throws Exception {
        Path filePath = Paths.get(FileUtils.class.getClassLoader().getResource("").toURI()).resolve(fileName);
        if (!Files.exists(filePath.getParent())) {
            Files.createDirectories(filePath.getParent());
        }
        Files.write(filePath, body.getBytes());
    }

    private static String getFileContent(ResourceLoader resourceLoader, String fileName) {
        Resource resource = resourceLoader.getResource(fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .collect(joining(lineSeparator()));
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot read resource: %s", resource.getDescription()), e);
        }
    }
}

