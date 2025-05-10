package com.bruce.youngman.controller;

import com.bruce.youngman.service.MarkdownProcessingService;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/markdown")
public class MarkdownController {

    private final MarkdownProcessingService markdownService;
    
    @Autowired
    public MarkdownController(MarkdownProcessingService markdownService) {
        this.markdownService = markdownService;
    }
    
    /**
     * Process a Markdown file from a specified path
     */
    @GetMapping("/process")
    public ResponseEntity<?> processMarkdownFile(@RequestParam String filePath) {
        try {
            List<TextSegment> segments = markdownService.processMarkdownDocument(filePath);
            
            List<Map<String, String>> result = segments.stream()
                .map(segment -> {
                    Map<String, String> segmentMap = new HashMap<>();
                    segmentMap.put("text", segment.text());
                    return segmentMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    /**
     * Upload and process a Markdown file
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAndProcessMarkdown(@RequestParam("file") MultipartFile file) {
        try {
            // Check if file is empty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please upload a file");
            }
            
            // Check if file is markdown
            if (!file.getOriginalFilename().endsWith(".md")) {
                return ResponseEntity.badRequest().body("Only Markdown files are supported");
            }
            
            // Save the uploaded file temporarily
            String fileName = UUID.randomUUID().toString() + ".md";
            Path tempPath = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
            Files.write(tempPath, file.getBytes());
            
            // Process the file
            List<TextSegment> segments = markdownService.processMarkdownDocument(tempPath.toString());
            
            // Delete the temporary file
            Files.delete(tempPath);
            
            // Return the results
            List<Map<String, String>> result = segments.stream()
                .map(segment -> {
                    Map<String, String> segmentMap = new HashMap<>();
                    segmentMap.put("text", segment.text());
                    return segmentMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
} 