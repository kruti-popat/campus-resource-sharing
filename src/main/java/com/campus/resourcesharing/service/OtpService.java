package com.campus.resourcesharing.service;

import com.campus.resourcesharing.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Value("${otp.expiry.minutes:5}")
    private int otpExpiryMinutes;

    private final SecureRandom random = new SecureRandom();

    // Store OTP data keyed by email
    private final ConcurrentHashMap<String, OtpData> otpStore = new ConcurrentHashMap<>();

    // Store pending registration data keyed by email
    private final ConcurrentHashMap<String, RegisterRequest> pendingRegistrations = new ConcurrentHashMap<>();

    public String generateOtp(String email, RegisterRequest registerRequest) {
        String otp = String.format("%06d", random.nextInt(1000000));
        otpStore.put(email, new OtpData(otp, LocalDateTime.now().plusMinutes(otpExpiryMinutes)));
        pendingRegistrations.put(email, registerRequest);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpData otpData = otpStore.get(email);
        if (otpData == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(otpData.expiryTime)) {
            // OTP expired, clean up
            otpStore.remove(email);
            pendingRegistrations.remove(email);
            return false;
        }
        return otpData.otp.equals(otp);
    }

    public RegisterRequest getPendingRegistration(String email) {
        return pendingRegistrations.get(email);
    }

    public void clearOtp(String email) {
        otpStore.remove(email);
        pendingRegistrations.remove(email);
    }

    // Inner class to hold OTP and its expiry
    private static class OtpData {
        final String otp;
        final LocalDateTime expiryTime;

        OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }
}
