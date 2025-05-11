package com.expense.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.expense.CustomUserDetails;
import com.expense.exception.EmailNotFoundException;
import com.expense.model.User;
import com.expense.repository.UserRepository;

@Component
public class CustomUserDetailService implements UserDetailsService{
	
	@Autowired
	private UserRepository userRepository;
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		User user=userRepository.findByEmail(email);
		if(Objects.isNull(user))
		{
			System.out.println("email not available");
			throw new EmailNotFoundException("email not found");
			
		}
		return new CustomUserDetails(user);
	}

}
