package com.expense.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.expense.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    public User findByEmail(String email);
}