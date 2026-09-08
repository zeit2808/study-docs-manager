package com.studydocs.manager.service.auth;

import com.studydocs.manager.config.RateLimitProperties;
import com.studydocs.manager.dto.auth.ForgotPasswordRequest;
import com.studydocs.manager.dto.auth.ResetPasswordRequest;
import com.studydocs.manager.entity.PasswordResetToken;
import com.studydocs.manager.entity.User;
import com.studydocs.manager.exception.BadRequestException;
import com.studydocs.manager.exception.TooManyRequestsException;
import com.studydocs.manager.repository.PasswordResetTokenRepository;
import com.studydocs.manager.repository.UserRepository;
import com.studydocs.manager.security.service.RateLimiterService;
import com.studydocs.manager.service.mail.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetService {

    private static final Logger log =
            LoggerFactory.getLogger(PasswordResetService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int OTP_LENGTH = 8;
    private static final int OTP_MAX_ATTEMPTS = 5;
    private static final int OTP_EXPIRY_MINUTES = 5;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiterService rateLimiterService;
    private final RateLimitProperties rateLimitProperties;
    private final TransactionTemplate transactionTemplate;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            RateLimiterService rateLimiterService,
            RateLimitProperties rateLimitProperties,
            TransactionTemplate transactionTemplate
    ) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiterService = rateLimiterService;
        this.rateLimitProperties = rateLimitProperties;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Luôn kết thúc bình thường nếu email không tồn tại (chống email enumeration).
     * Nếu email tồn tại:
     * - Áp dụng Resend Cooldown (mặc định 60s giữa 2 lần gửi liên tiếp). Nếu vi phạm -> ném TooManyRequestsException (HTTP 429).
     * - Áp dụng giới hạn số lần gửi trong 1 giờ (mặc định 3 lần/giờ). Nếu vi phạm -> ném TooManyRequestsException (HTTP 429).
     * - Nếu hợp lệ -> hủy token cũ, tạo OTP mới và gửi email bất đồng bộ ngầm.
     */
    public void sendOtp(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();

        OtpDispatchPayload payload = transactionTemplate.execute(status -> {
            Optional<User> userOptional = userRepository.findByEmailForUpdate(email);

            // Không throw lỗi nếu email không tồn tại (chống email enumeration)
            if (userOptional.isEmpty()) {
                return null;
            }

            User user = userOptional.get();
            LocalDateTime now = LocalDateTime.now();

            // 1. Kiểm tra Resend Cooldown giữa các lần gửi liên tiếp (ví dụ 60 giây)
            Optional<PasswordResetToken> latestToken =
                    passwordResetTokenRepository.findTop1ByUserOrderByCreatedAtDesc(user);

            if (latestToken.isPresent()) {
                LocalDateTime createdAt = latestToken.get().getCreatedAt();
                if (createdAt != null) {
                    long secondsSinceLast = Duration.between(createdAt, now).getSeconds();
                    int cooldownSeconds = rateLimitProperties.getForgotPasswordResendCooldownSeconds();
                    if (secondsSinceLast < cooldownSeconds) {
                        long remainingSeconds = cooldownSeconds - secondsSinceLast;
                        throw new TooManyRequestsException(
                                "Please wait " + remainingSeconds + " seconds before requesting a new code.",
                                "RESEND_COOLDOWN",
                                "email",
                                remainingSeconds
                        );
                    }
                }
            }

            // 2. Kiểm tra giới hạn số lần gửi trong 1 giờ (ví dụ tối đa 3 lần/giờ)
            boolean underEmailLimit = rateLimiterService.tryConsume(
                    "forgot-password-email:" + user.getId(),
                    rateLimitProperties.getForgotPasswordPerEmailPerHour(),
                    Duration.ofHours(1)
            );

            if (!underEmailLimit) {
                throw new TooManyRequestsException(
                        "You have requested password reset too many times. Please try again in 1 hour.",
                        "RATE_LIMIT_EXCEEDED",
                        "email"
                );
            }

            passwordResetTokenRepository.deleteByUser(user);

            String otp = generateOtp();
            String otpHash = passwordEncoder.encode(otp);
            LocalDateTime expiredAt = now.plusMinutes(OTP_EXPIRY_MINUTES);

            passwordResetTokenRepository.save(
                    new PasswordResetToken(user, otpHash, expiredAt)
            );

            return new OtpDispatchPayload(user.getId(), user.getEmail(), otp);
        });

        if (payload != null) {
            // Gửi email bất đồng bộ ngầm
            emailService.sendOtpEmailAsync(payload.email(), payload.otp(), OTP_EXPIRY_MINUTES);
            log.info("Password reset code generated and async email dispatched for userId={}", payload.userId());
        }
    }

    public User resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException(
                    "Password confirmation does not match",
                    "PASSWORD_CONFIRMATION_MISMATCH",
                    "confirmNewPassword"
            );
        }

        String email = request.getEmail().trim();

        // Thực thi kiểm tra và cập nhật OTP trong transaction để đảm bảo attempt_count luôn được COMMIT vào DB
        ResetPasswordResult result = transactionTemplate.execute(status -> {
            Optional<User> userOptional = userRepository.findByEmailForUpdate(email);
            if (userOptional.isEmpty()) {
                return ResetPasswordResult.invalid();
            }

            User user = userOptional.get();

            Optional<PasswordResetToken> tokenOptional = passwordResetTokenRepository
                    .findTop1ByUserAndExpiredAtAfterOrderByCreatedAtDesc(
                            user,
                            LocalDateTime.now()
                    );

            if (tokenOptional.isEmpty()) {
                return ResetPasswordResult.invalid();
            }

            PasswordResetToken token = tokenOptional.get();

            // Nếu đã vượt quá số lần thử từ trước
            if (token.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                passwordResetTokenRepository.deleteByUser(user);
                return ResetPasswordResult.attemptsExceeded();
            }

            // Kiểm tra mã OTP
            if (!passwordEncoder.matches(request.getOtp(), token.getOtpHash())) {
                token.incrementAttemptCount();
                if (token.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
                    passwordResetTokenRepository.deleteByUser(user);
                    return ResetPasswordResult.attemptsExceeded();
                } else {
                    passwordResetTokenRepository.save(token);
                    int remaining = OTP_MAX_ATTEMPTS - token.getAttemptCount();
                    return ResetPasswordResult.wrongOtp(remaining);
                }
            }

            // Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
            if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
                return ResetPasswordResult.samePassword();
            }

            // Mọi điều kiện hợp lệ: Cập nhật mật khẩu mới và tăng tokenVersion
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.incrementTokenVersion();

            User savedUser = userRepository.save(user);
            passwordResetTokenRepository.deleteByUser(user);

            return ResetPasswordResult.success(savedUser);
        });

        // Xử lý ném Exception SAU KHI Transaction đã COMMIT vào MySQL thành công
        if (result == null || result.isInvalid()) {
            throw invalidResetCode();
        }

        if (result.isAttemptsExceeded()) {
            throw new BadRequestException(
                    "Too many failed attempts. This reset code has been invalidated. Please request a new one.",
                    "OTP_ATTEMPTS_EXCEEDED",
                    "otp"
            );
        }

        if (result.isWrongOtp()) {
            throw new BadRequestException(
                    "Invalid reset code. You have " + result.remainingAttempts() + " attempt(s) remaining.",
                    "INVALID_RESET_CODE",
                    "otp"
            );
        }

        if (result.isSamePassword()) {
            throw new BadRequestException(
                    "New password must be different from current password",
                    "SAME_PASSWORD",
                    "newPassword"
            );
        }

        // Gửi email thông báo mật khẩu thay đổi qua luồng ngầm bất đồng bộ
        emailService.sendPasswordChangedEmailAsync(result.user().getEmail());

        return result.user();
    }

    private BadRequestException invalidResetCode() {
        return new BadRequestException(
                "Invalid or expired reset code",
                "INVALID_RESET_CODE",
                "otp"
        );
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int maxExclusive = (int) Math.pow(10, OTP_LENGTH);

        return String.valueOf(
                min + SECURE_RANDOM.nextInt(maxExclusive - min)
        );
    }

    private record OtpDispatchPayload(Long userId, String email, String otp) {}

    private record ResetPasswordResult(
            Status status,
            User user,
            int remainingAttempts
    ) {
        enum Status {
            SUCCESS,
            INVALID,
            WRONG_OTP,
            ATTEMPTS_EXCEEDED,
            SAME_PASSWORD
        }

        static ResetPasswordResult success(User user) {
            return new ResetPasswordResult(Status.SUCCESS, user, 0);
        }

        static ResetPasswordResult invalid() {
            return new ResetPasswordResult(Status.INVALID, null, 0);
        }

        static ResetPasswordResult wrongOtp(int remainingAttempts) {
            return new ResetPasswordResult(Status.WRONG_OTP, null, remainingAttempts);
        }

        static ResetPasswordResult attemptsExceeded() {
            return new ResetPasswordResult(Status.ATTEMPTS_EXCEEDED, null, 0);
        }

        static ResetPasswordResult samePassword() {
            return new ResetPasswordResult(Status.SAME_PASSWORD, null, 0);
        }

        boolean isSuccess() { return status == Status.SUCCESS; }
        boolean isInvalid() { return status == Status.INVALID; }
        boolean isWrongOtp() { return status == Status.WRONG_OTP; }
        boolean isAttemptsExceeded() { return status == Status.ATTEMPTS_EXCEEDED; }
        boolean isSamePassword() { return status == Status.SAME_PASSWORD; }
    }
}
