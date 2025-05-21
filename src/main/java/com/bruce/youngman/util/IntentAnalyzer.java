package com.bruce.youngman.util;

import com.bruce.youngman.model.IntentVO;
import com.bruce.youngman.service.ChatService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IntentAnalyzer {

    private final ChatService chatService;

    public IntentAnalyzer(ChatService chatService) {
        this.chatService = chatService;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // Convert numeric to string without scientific notation
                    return String.valueOf((long)cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    public void analyzeIntents(String inputFile, String outputFile) throws IOException {
        List<String> messages = new ArrayList<>();
        List<IntentVO> results = new ArrayList<>();

        // Read input Excel
        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(0);
                String message = getCellValueAsString(cell);
                if (!message.isEmpty()) {
                    messages.add(message);
                    // Call confirmIndent for each message
                    IntentVO result = chatService.confirmIndent(message);
                    results.add(result);
                }
            }
        }

        // Write results to output Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Intent Analysis Results");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Original Message");
            headerRow.createCell(1).setCellValue("Detected Intent");
            headerRow.createCell(2).setCellValue("Intent Result");
            headerRow.createCell(3).setCellValue("Thought Chain");

            // Write data rows
            for (int i = 0; i < messages.size(); i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(messages.get(i));
                row.createCell(1).setCellValue(results.get(i).getIntent());
                row.createCell(2).setCellValue(results.get(i).getIntendResult());
                row.createCell(3).setCellValue(results.get(i).getThoughtChain());
            }

            // Auto-size columns
            for (int i = 0; i < 4; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
} 