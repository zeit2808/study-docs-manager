package com.studydocs.manager.service;

import com.studydocs.manager.dto.ForgotPasswordRequest;
import com.studydocs.manager.dto.ResetPasswordRequest;
import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.PasswordResetTokenRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository passwordResetTokenRepository,
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }


    @Transactional
    public void sendOtp(ForgotPasswordRequest request) {
        Optional<User> optionalUser = userRepository.findByEmail(request.getEmail());
        if (optionalUser.isEmpty()) {
            // Không ném lỗi ra ngoài để tránh lộ thông tin
            return;
        }

        User user = optionalUser.get();

        String otp = generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(10);

        PasswordResetToken token = new PasswordResetToken(user, otp, expiredAt);
        passwordResetTokenRepository.save(token);

        sendOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        PasswordResetToken token = passwordResetTokenRepository
                .findToByUserAndUsedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new RuntimeException("No OTP request found"));

        if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        if (!token.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // OTP đúng
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
    }

    private String generateOtp() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6 chữ số
        return String.valueOf(code);
    }

    private void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset Password OTP");
        message.setText("Your OTP code is: " + otp + "\nThis code is valid for 10 minutes.");
        mailSender.send(message);
    }
}