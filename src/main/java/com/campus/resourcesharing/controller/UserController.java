package com.campus.resourcesharing.controller;

import com.campus.resourcesharing.dto.ApiResponse;
import com.campus.resourcesharing.dto.LoginRequest;
import com.campus.resourcesharing.dto.RegisterRequest;
import com.campus.resourcesharing.dto.VerifyOtpRequest;
import com.campus.resourcesharing.model.User;
import com.campus.resourcesharing.service.EmailService;
import com.campus.resourcesharing.service.OtpService;
import com.campus.resourcesharing.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OtpService otpService;
    
    @Autowired
    private EmailService emailService;
    
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest request) {
        try {
            // Validate that username and email don't already exist
            userService.validateRegistration(request);
            
            // Generate OTP and send email
            String otp = otpService.generateOtp(request.getEmail(), request);
            emailService.sendOtpEmail(request.getEmail(), otp);
            
            Map<String, Object> data = new HashMap<>();
            data.put("email", request.getEmail());
            
            return ResponseEntity.ok(new ApiResponse(true, "OTP sent to your email. Please verify to complete registration.", data));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            boolean isValid = otpService.verifyOtp(request.getEmail(), request.getOtp());
            
            if (!isValid) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Invalid or expired OTP"));
            }
            
            // OTP verified, now create the user
            RegisterRequest registerRequest = otpService.getPendingRegistration(request.getEmail());
            if (registerRequest == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Registration data not found. Please register again."));
            }
            
            User user = userService.register(registerRequest);
            otpService.clearOtp(request.getEmail());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            
            return ResponseEntity.ok(new ApiResponse(true, "Email verified! Registration successful.", userData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            RegisterRequest pendingRequest = otpService.getPendingRegistration(email);
            
            if (pendingRequest == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "No pending registration found for this email. Please register again."));
            }
            
            // Generate new OTP and resend
            String otp = otpService.generateOtp(email, pendingRequest);
            emailService.sendOtpEmail(email, otp);
            
            return ResponseEntity.ok(new ApiResponse(true, "OTP resent to your email."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            
            return ResponseEntity.ok(new ApiResponse(true, "Login successful", userData));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("phone", user.getPhone());
            userData.put("bio", user.getBio());
            userData.put("department", user.getDepartment());
            userData.put("year", user.getYear());
            userData.put("averageRating", user.getAverageRating());
            userData.put("totalRatings", user.getTotalRatings());
            userData.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(new ApiResponse(true, "User found", userData));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/profile")
    public ResponseEntity<ApiResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> profileData) {
        try {
            User user = userService.updateProfile(
                    id,
                    profileData.get("fullName"),
                    profileData.get("phone"),
                    profileData.get("bio"),
                    profileData.get("department"),
                    profileData.get("year")
            );
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("fullName", user.getFullName());
            userData.put("phone", user.getPhone());
            userData.put("bio", user.getBio());
            userData.put("department", user.getDepartment());
            userData.put("year", user.getYear());
            userData.put("averageRating", user.getAverageRating());
            userData.put("totalRatings", user.getTotalRatings());
            
            return ResponseEntity.ok(new ApiResponse(true, "Profile updated successfully", userData));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }
}
