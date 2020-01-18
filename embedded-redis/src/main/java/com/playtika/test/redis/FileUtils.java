package com.playtika.test.redis;

import lombok.experimental.UtilityClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class FileUtils {

    public static String getFileContent(String filePath) {
        try (Stream<String> lines = getFileStream(filePath)) {
            return lines.collect(Collectors.joining("\n"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load file - " + filePath, e);
        }
    }

    public static void resolveTemplate(String fileName, Function<String, String> modifyFunc) throws Exception {
        String fileTemplateContent = FileUtils.getFileContent(fileName + ".template");
        String modifiedFile = modifyFunc.apply(fileTemplateContent);
        FileUtils.writeToFileInClassesDir(modifiedFile, fileName);
    }

    public static void writeToFileInClassesDir(String body, String fileName) throws Exception {
        Path url = Paths.get("target", "classes", fileName);
        Files.write(url, body.getBytes());
    }

    private static Stream<String> getFileStream(String filePath) throws Exception {
        Path path = Paths.get(FileUtils.class.getClassLoader().getResource(filePath).toURI());
        return Files.lines(path);
    }
}

