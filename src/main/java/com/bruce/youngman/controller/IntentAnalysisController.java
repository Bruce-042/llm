package com.bruce.youngman.controller;

import com.bruce.youngman.util.IntentAnalyzer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/intent")
public class IntentAnalysisController {

    private final IntentAnalyzer intentAnalyzer;

    public IntentAnalysisController(IntentAnalyzer intentAnalyzer) {
        this.intentAnalyzer = intentAnalyzer;
    }

    @GetMapping("/analyze")
    public String analyzeIntents() {
        try {
            String inputFile = "src/main/resources/documents/意图确认标注数据.xlsx";
            String outputFile = "src/main/resources/documents/意图分析结果.xlsx";
            intentAnalyzer.analyzeIntents(inputFile, outputFile);
            return "Analysis completed. Results saved to: " + outputFile;
        } catch (IOException e) {
            return "Error during analysis: " + e.getMessage();
        }
    }
} 