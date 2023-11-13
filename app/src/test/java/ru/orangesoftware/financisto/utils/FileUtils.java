package ru.orangesoftware.financisto.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class FileUtils {

    public static String testFileAsString(String fileName) {
        try {
            InputStream is = ClassLoader.getSystemResourceAsStream(fileName);
            return new Scanner(is, StandardCharsets.UTF_8).useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
