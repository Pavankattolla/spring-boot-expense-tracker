package com.expense.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.expense.model.Expense;
import com.expense.model.User;

@Service
public class ExpenseServiceImpl implements ExpenseServiceI {

    @Autowired
    private GoogleSheetService googleSheetService;

    @Autowired
    private UserService userService;

    @Override
    public void addExpense(Expense expense, String username) throws IOException {
        try {
            User user = userService.getUserByEmail(username);
            if (user != null && user.getSpreadsheetId() != null) {
                googleSheetService.addExpense(user.getSpreadsheetId(), user.getUsername(), expense); // Pass username
            } else {
                throw new IOException("User not found or spreadsheet ID not set.");
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Google Sheets error", e);
        }
    }
    

    @Override
    public InputStream getExpenseFile(String username) throws IOException {
        try {
            User user = userService.getUserByEmail(username);
            if (user != null && user.getSpreadsheetId() != null) {
                return googleSheetService.getExpenseFile(user.getSpreadsheetId());
            } else {
                throw new IOException("User not found or spreadsheet ID not set.");
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("Error accessing Google Sheets: " + e.getMessage(), e);
        }
    }
}