package com.expense.model;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Expense {
	
	private LocalDate date;
	private String category;
	private double amount;
	private String description;

}
