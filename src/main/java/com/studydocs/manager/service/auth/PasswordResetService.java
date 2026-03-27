package com.studydocs.manager.service.auth;

import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.repository.PasswordResetTokenRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

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
            return;
        }

        User user = optionalUser.get();

        String otp = generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(5);

        PasswordResetToken token = new PasswordResetToken(user, otp, expiredAt);
        passwordResetTokenRepository.save(token);

        try {
            sendOtpEmail(user.getEmail(), otp);
        } catch (Exception e) {
            log.error("Send OTP email failed for user: {}", user.getEmail(), e);
        }
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

    private void sendOtpEmail(String toEmail, String otp) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        String subject = "Reset Your Password - OTP Code";
        String content = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8">
                </head>
                <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,sans-serif;">

                    <div style="max-width:520px;margin:40px auto;background:#ffffff;border-radius:10px;
                                box-shadow:0 4px 12px rgba(0,0,0,0.1);overflow:hidden;">

                        <!-- Header -->
                        <div style="background:#2d89ef;color:white;padding:16px 20px;font-size:20px;font-weight:bold;">
                            🔐 Password Reset
                        </div>

                        <!-- Body -->
                        <div style="padding:24px;">
                            <p style="font-size:14px;color:#333;">Hello,</p>

                            <p style="font-size:14px;color:#333;">
                                We received a request to reset your password.
                            </p>

                            <p style="font-size:14px;color:#333;">
                                Use the OTP code below to continue:
                            </p>

                            <!-- OTP BOX -->
                            <div style="text-align:center;margin:24px 0;">
                                <span style="display:inline-block;padding:12px 24px;
                                             font-size:30px;font-weight:bold;
                                             letter-spacing:4px;
                                             color:#2d89ef;
                                             border:2px dashed #2d89ef;
                                             border-radius:8px;">
                                    %s
                                </span>
                            </div>

                            <p style="font-size:14px;color:#333;">
                                This code will expire in <b>5 minutes</b>.
                            </p>

                            <p style="font-size:14px;color:#333;">
                                If you did not request this, you can safely ignore this email.
                            </p>

                            <hr style="border:none;border-top:1px solid #eee;margin:20px 0;"/>

                            <p style="font-size:12px;color:#888;text-align:center;">
                                ⚠️ Never share your OTP with anyone.
                            </p>
                        </div>
                    </div>

                </body>
                </html>
                """.formatted(otp);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
    }
}