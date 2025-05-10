package com.bruce.youngman.service;

import com.bruce.youngman.util.MdDocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Service for processing Markdown documents using LangChain4j
 */
@Service
public class MarkdownProcessingService {
    
    /**
     * Alternative method using custom implementation
     */
    public List<TextSegment> processMarkdownDocumentCustom(String filePath) throws Exception {
        // Validate file path
        Path path = Paths.get(filePath);
        if (!path.toString().toLowerCase().endsWith(".md")) {
            throw new IllegalArgumentException("File must be a Markdown (.md) file");
        }
        
        // Use custom implementation
        return MdDocumentSplitter.splitMarkdownByTitles(filePath);
    }

    public List<TextSegment> processMarkdownDocument(String filePath) {
        return MdDocumentSplitter.splitMarkdownByTitles(filePath);
    }
}