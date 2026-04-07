package com.campus.resourcesharing.service;

import com.campus.resourcesharing.dto.LoginRequest;
import com.campus.resourcesharing.dto.RegisterRequest;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void validateRegistration(RegisterRequest request) {
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
    }
    
    public User register(RegisterRequest request) {
        // Validate before creating
        validateRegistration(request);
        
        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword()); // In production, hash the password
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        
        return userRepository.save(user);
    }
    
    public User login(LoginRequest request) {
        Optional<User> userOptional = userRepository.findByUsername(request.getUsername());
        
        if (userOptional.isEmpty()) {
            throw new RuntimeException("Invalid username or password");
        }
        
        User user = userOptional.get();
        
        // In production, use password hashing (BCrypt)
        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }
        
        return user;
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User updateProfile(Long userId, String fullName, String phone, String bio, 
                              String department, String year) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName.trim());
        }
        if (phone != null) {
            user.setPhone(phone.trim());
        }
        if (bio != null) {
            user.setBio(bio.trim());
        }
        if (department != null) {
            user.setDepartment(department.trim());
        }
        if (year != null) {
            user.setYear(year.trim());
        }
        
        return userRepository.save(user);
    }
}
