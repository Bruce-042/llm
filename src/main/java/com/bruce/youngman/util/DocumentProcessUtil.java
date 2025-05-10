package com.bruce.youngman.util;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Liangyonghui
 * @since 2025/5/9 16:29
 */
public class DocumentProcessUtil {
    public static Path toPath(String relativePath) {
        try {
            URL fileUrl = DocumentProcessUtil.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
