package com.bruce.youngman.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import org.apache.commons.lang3.StringUtils;

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
import java.util.stream.Collectors;

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

        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<TextSegment> segments = new ArrayList<>();
        
        // 使用正则表达式匹配标题和内容
        Pattern pattern = Pattern.compile("#\\s+(.+?)\\n((?:.|\\n)*?)(?=#\\s+|$)", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(fileContent);
        
        while (matcher.find()) {
            String title = matcher.group(1).trim();  // 标题文本
            String content = matcher.group(2).trim();  // 内容
            if (StringUtils.isBlank(content)) {
                continue;
            }
            
            // 创建metadata并添加标题
            Metadata metadata = new Metadata();
            metadata.put("title", title);
            
            // 创建TextSegment，包含内容和标题元数据
            TextSegment segment = TextSegment.from(content, metadata);
            segments.add(segment);
        }

        return segments;
    }

    public static List<Document> splitMarkdownByTitlesAndGetDocuments(String mdFilePath)  {
        List<TextSegment> textSegments = splitMarkdownByTitles(mdFilePath);
        return textSegments.stream().map(e -> Document.from(e.text(), e.metadata())).collect(Collectors.toList());
    }
    

} 