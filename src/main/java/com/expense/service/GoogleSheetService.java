package com.expense.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.expense.model.Expense;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class GoogleSheetService {
	
	
    private static final String CREDENTIALS_PATH = "/credentials.json";  // in resources/

    private Sheets getSheetsService() throws IOException, GeneralSecurityException {
        InputStream in = getClass().getResourceAsStream(CREDENTIALS_PATH);
        GoogleCredential credential = GoogleCredential.fromStream(in)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        return new Sheets.Builder(credential.getTransport(), credential.getJsonFactory(), credential)
                .setApplicationName("Expense Tracker")
                .build();
    }

    public String createSpreadsheet(String username) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        Spreadsheet spreadsheet = new Spreadsheet()
                .setProperties(new SpreadsheetProperties().setTitle(username + " Expenses"));
        Spreadsheet createdSpreadsheet = service.spreadsheets().create(spreadsheet).execute();
        String spreadsheetId = createdSpreadsheet.getSpreadsheetId();
        ensureSheetExists(service, spreadsheetId, username + " - " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM")));
        return spreadsheetId;
    }

    public void addExpense(String spreadsheetId, String username, Expense expense) throws IOException, GeneralSecurityException {
        Sheets sheetsService = getSheetsService();

        String monthSheetName = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM"));
        String sheetName = username + " - " + monthSheetName;

        ensureSheetExists(sheetsService, spreadsheetId, sheetName);

        String date = (expense.getDate() != null) ? expense.getDate().toString() : LocalDate.now().toString();
        List<Object> row = Arrays.asList(date, expense.getCategory(), expense.getAmount(), expense.getDescription());

        ValueRange appendBody = new ValueRange().setValues(Collections.singletonList(row));
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, sheetName + "!A:D", appendBody)
                .setValueInputOption("USER_ENTERED")
                .execute();

        updateTotal(sheetsService, spreadsheetId, sheetName);
    }

    private void ensureSheetExists(Sheets service, String spreadsheetId, String sheetName) throws IOException {
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        boolean sheetExists = spreadsheet.getSheets().stream()
                .anyMatch(sheet -> sheet.getProperties().getTitle().equals(sheetName));

        if (!sheetExists) {
            // Create Sheet
            AddSheetRequest addSheetRequest = new AddSheetRequest()
                    .setProperties(new SheetProperties().setTitle(sheetName));
            Request request = new Request().setAddSheet(addSheetRequest);
            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest().setRequests(Collections.singletonList(request));
            service.spreadsheets().batchUpdate(spreadsheetId, body).execute();

            // Add headers
            List<List<Object>> headerRows = Arrays.asList(
                    Arrays.asList("", "Total Amount", 0),
                    Arrays.asList("Date", "Category", "Amount", "Description")
            );
            ValueRange headers = new ValueRange().setValues(headerRows);
            service.spreadsheets().values()
                    .update(spreadsheetId, sheetName + "!A1", headers)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

    private void updateTotal(Sheets service, String spreadsheetId, String sheetName) throws IOException {
        // Read all values
        ValueRange data = service.spreadsheets().values()
                .get(spreadsheetId, sheetName + "!C3:C")
                .execute();

        List<List<Object>> values = data.getValues();
        double total = 0;
        if (values != null) {
            for (List<Object> row : values) {
                try {
                    total += Double.parseDouble(row.get(0).toString());
                } catch (Exception ignored) {}
            }
        }

        ValueRange totalUpdate = new ValueRange()
                .setValues(Collections.singletonList(Collections.singletonList(total)));
        service.spreadsheets().values()
                .update(spreadsheetId, sheetName + "!C1", totalUpdate)
                .setValueInputOption("RAW")
                .execute();
    }

    public InputStream getExpenseFile(String spreadsheetId) throws IOException, GeneralSecurityException {
        Sheets service = getSheetsService();
        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        
        // Create workbook
        Workbook workbook = new XSSFWorkbook();
        
        // For each sheet in Google Sheets
        for (Sheet sheet : spreadsheet.getSheets()) {
            String sheetName = sheet.getProperties().getTitle();
            org.apache.poi.ss.usermodel.Sheet xlsSheet = workbook.createSheet(sheetName);
            
            // Get all data from the sheet
            ValueRange response = service.spreadsheets().values()
                    .get(spreadsheetId, sheetName)
                    .execute();
            List<List<Object>> values = response.getValues();
            
            if (values != null) {
                int rowNum = 0;
                for (List<Object> rowData : values) {
                    Row row = xlsSheet.createRow(rowNum++);
                    int cellNum = 0;
                    for (Object cellData : rowData) {
                        Cell cell = row.createCell(cellNum++);
                        if (cellData != null) {
                            String value = cellData.toString();
                            // Try to parse as number if possible
                            try {
                                double numValue = Double.parseDouble(value);
                                cell.setCellValue(numValue);
                            } catch (NumberFormatException e) {
                                cell.setCellValue(value);
                            }
                        }
                    }
                }
                
                // Auto size columns
                for (int i = 0; i < 4; i++) {
                    xlsSheet.autoSizeColumn(i);
                }
            }
        }
        
        // Convert to InputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}