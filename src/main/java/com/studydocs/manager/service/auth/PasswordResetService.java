package com.studydocs.manager.service.auth;

import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.NotFoundException;
import com.studydocs.manager.exception.ServiceUnavailableException;
import com.studydocs.manager.exception.TooManyRequestsException;
import com.studydocs.manager.repository.PasswordResetTokenRepository;
import com.studydocs.manager.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            log.warn("Forgot-password: email not found -> [{}]", request.getEmail());
            throw new NotFoundException(
                    "No account is associated with this email",
                    "EMAIL_NOT_FOUND",
                    "email");
        }

        User user = optionalUser.get();

        Optional<PasswordResetToken> activeToken =
                passwordResetTokenRepository.findTop1ByUserAndExpiredAtAfterOrderByCreatedAtDesc(
                        user, LocalDateTime.now());

        if (activeToken.isPresent()) {
            long secondsLeft = java.time.Duration.between(
                    LocalDateTime.now(), activeToken.get().getExpiredAt()).getSeconds();
            long minutesLeft = (secondsLeft + 59) / 60;
            throw new TooManyRequestsException(
                    "An OTP was already sent. Please wait " + minutesLeft
                            + " minute(s) before requesting a new one.",
                    "OTP_COOLDOWN",
                    null);
        }
        passwordResetTokenRepository.deleteByUser(user);
        String otp = generateOtp();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(5);
        passwordResetTokenRepository.save(new PasswordResetToken(user, otp, expiredAt));

        try {
            sendOtpEmail(user.getEmail(), otp);
            log.info("Forgot-password: OTP sent -> [{}], expires at {}", user.getEmail(), expiredAt);
        } catch (Exception e) {
            log.error("Forgot-password: failed to send email -> [{}]", user.getEmail(), e);
            throw new ServiceUnavailableException(
                    "Failed to send OTP email, please try again later",
                    "EMAIL_SEND_FAILED",
                    "email");
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found", "USER_NOT_FOUND", "email"));

        PasswordResetToken token = passwordResetTokenRepository
                .findTop1ByUserAndOtpOrderByCreatedAtDesc(user, request.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid OTP", "INVALID_OTP", "otp"));

        if (token.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP expired", "OTP_EXPIRED", "otp");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        passwordResetTokenRepository.deleteByUser(user);
    }

    private String generateOtp() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
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
                        <div style="background:#2d89ef;color:white;padding:16px 20px;font-size:20px;font-weight:bold;">
                            Password Reset
                        </div>
                        <div style="padding:24px;">
                            <p style="font-size:14px;color:#333;">Hello,</p>
                            <p style="font-size:14px;color:#333;">
                                We received a request to reset your password.
                            </p>
                            <p style="font-size:14px;color:#333;">
                                Use the OTP code below to continue:
                            </p>
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
                                Never share your OTP with anyone.
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
