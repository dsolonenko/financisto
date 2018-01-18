package ru.orangesoftware.financisto.utils;

import java.io.InputStream;
import java.util.Scanner;

import static android.support.test.InstrumentationRegistry.getInstrumentation;

public class FileUtils {

    public static String testFileAsString(String fileName) {
        try {
            InputStream is = getInstrumentation().getContext().getResources().getAssets().open(fileName);
            return new Scanner(is, "UTF-8").useDelimiter("\\A").next();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
