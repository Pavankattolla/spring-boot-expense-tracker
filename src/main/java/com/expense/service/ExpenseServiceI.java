package com.expense.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import com.expense.model.Expense;
import com.expense.model.User;

public interface ExpenseServiceI {
    void addExpense(Expense expense, String username) throws IOException;
	
    InputStream getExpenseFile(String username) throws IOException;
    

}
