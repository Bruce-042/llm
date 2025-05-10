package com.bruce.youngman.util;


import dev.langchain4j.data.segment.TextSegment;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class for splitting Markdown documents into segments where each title and its
 * associated text forms a single segment.
 */
public class MdDocumentSplitter {

    /**
     * Splits a Markdown document into segments where each segment contains a title and its associated text.
     *
     * @param mdFilePath Path to the Markdown file
     * @return List of TextSegment objects
     */
    public static List<TextSegment> splitMarkdownByTitles(String mdFilePath)  {
        Path path = null;
        try {
            URL fileUrl = MdDocumentSplitter.class.getClassLoader().getResource(mdFilePath);
            path = Paths.get(fileUrl.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        String content = null;
        try {
            content = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<TextSegment> segments = new ArrayList<>();
        
        // Pattern to match markdown titles (# Title) and the content that follows until the next title
        Pattern pattern = Pattern.compile("(#\\s+.+?)(?=(#\\s+|$))", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        
        while (matcher.find()) {
            String segment = matcher.group(1).trim();
            segments.add(TextSegment.from(segment));
        }
        
        return segments;
    }
    

} 