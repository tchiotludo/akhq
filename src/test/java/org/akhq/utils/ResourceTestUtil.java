package org.akhq.utils;

import com.google.common.io.CharStreams;
import org.junit.platform.commons.util.ClassLoaderUtils;

import java.io.IOException;
import java.io.InputStreamReader;

public class ResourceTestUtil {

    public static String resourceAsString(String resourcePath) throws IOException {
        try (
            InputStreamReader isr = new InputStreamReader(ClassLoaderUtils.getDefaultClassLoader().getResourceAsStream(resourcePath))
        ) {
            return CharStreams.toString(isr);
        }
    }
}
