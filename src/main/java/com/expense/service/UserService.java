package com.expense.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import this

import com.expense.model.User;
import com.expense.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private GoogleSheetService googleSheetService;

    @Transactional // Add this annotation
    public void add(User user) {
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        try {
            String spreadsheetId = googleSheetService.createSpreadsheet(user.getUsername());
            user.setSpreadsheetId(spreadsheetId);
            userRepository.save(user);
            System.out.println("User saved with spreadsheetId: " + spreadsheetId); // Add this line
        } catch (IOException | GeneralSecurityException e) {
            // Handle the error appropriately, maybe rollback user creation or log it
            System.err.println("Error creating Google Sheet for user: " + e.getMessage());
            throw new RuntimeException("Error creating spreadsheet for user", e);
        }
    }

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getUserByEmail(String email) {
        User user = userRepository.findByEmail(email);
        System.out.println("Retrieved user: " + user); // Add this line
        return user;
    }
    

}