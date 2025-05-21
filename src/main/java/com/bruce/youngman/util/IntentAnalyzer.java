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
        List<String> expectedResults = new ArrayList<>();
        List<IntentVO> results = new ArrayList<>();

        // Read input Excel
        try (FileInputStream fis = new FileInputStream(inputFile);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell messageCell = row.getCell(0);
                Cell expectedResultCell = row.getCell(1);
                String message = getCellValueAsString(messageCell);
                String expectedResult = getCellValueAsString(expectedResultCell);
                
                if (!message.isEmpty()) {
                    messages.add(message);
                    expectedResults.add(expectedResult);
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
            headerRow.createCell(0).setCellValue("原始提问");
            headerRow.createCell(1).setCellValue("识别结果");
            headerRow.createCell(2).setCellValue("是否与预期一致");
            headerRow.createCell(3).setCellValue("期望结果");
            headerRow.createCell(4).setCellValue("猜测询问");
            headerRow.createCell(5).setCellValue("思维过程");


            // Write data rows
            for (int i = 0; i < messages.size(); i++) {
                Row row = sheet.createRow(i + 1);
                String actualResult = results.get(i).getIntendResult();
                String expectedResult = expectedResults.get(i);
                String tempExpectedResult = expectedResult;
                if ("模糊".equals(expectedResult)) {
                    tempExpectedResult = "不明确";
                }
                boolean matches = actualResult.equals(tempExpectedResult);
                
                row.createCell(0).setCellValue(messages.get(i));
                row.createCell(1).setCellValue(actualResult);
                row.createCell(2).setCellValue(matches ? "是" : "否");
                row.createCell(3).setCellValue(expectedResult);
                row.createCell(4).setCellValue(results.get(i).getIntent());
                row.createCell(5).setCellValue(results.get(i).getThoughtChain());
            }

            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }
        }
    }
} 