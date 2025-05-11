package com.expense.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

import com.expense.model.Expense;
import com.expense.model.User;
import com.expense.service.ExpenseServiceI;
import com.expense.service.UserService;

@RestController
@RequestMapping("/expensetracker")
public class ExpenseController {

    @Autowired
    private ExpenseServiceI expenseServiceI;

    @Autowired
    private UserService userService;

    @PostMapping("/add")
    public ResponseEntity<String> addExpense(@RequestBody Expense expense) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        try {
            expenseServiceI.addExpense(expense, username);
            return ResponseEntity.ok("Expense added to your Google Sheet.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
    



//    @GetMapping("/download")
//    public ResponseEntity<InputStreamResource> downloadExcel() throws IOException {
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        String username = auth.getName();
//
//        InputStreamResource file = new InputStreamResource(expenseServiceI.getExpenseFile(username));
//        HttpHeaders headers = new HttpHeaders();
//        headers.add("Content-Disposition", "attachment; filename=expenses_" + username + ".xlsx"); // This will likely throw UnsupportedOperationException
//
//        return ResponseEntity.ok()
//                .headers(headers)
//                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
//                .body(file);
//    }
    @GetMapping("/download")
    public ResponseEntity<InputStreamResource> downloadExcel() throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        InputStreamResource file = new InputStreamResource(expenseServiceI.getExpenseFile(username));
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=expenses_" + username + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }



    @GetMapping("/me")
    public String getLoginedInUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return "Welcome " + authentication.getName();
    }

    @PostMapping("/register")
    public void addUser(@RequestBody User user) {
        userService.add(user);
    }
}