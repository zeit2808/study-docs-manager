package com.studydocs.manager.service.mail;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${mail.from.email:${spring.mail.username:}}")
    private String fromEmail;

    @Value("${mail.from.name:StudyDocs Support}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Gửi mã OTP bất đồng bộ, giải phóng hoàn toàn HTTP thread của API.
     */
    @Async("mailTaskExecutor")
    public void sendOtpEmailAsync(String toEmail, String otp, int expiryMinutes) {
        long startTime = System.currentTimeMillis();
        log.info("[Async Mail] Starting sending OTP email to {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail, fromName);
            }
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu - StudyDocs");
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                        <h2 style="color: #2563eb; text-align: center;">Yêu cầu đặt lại mật khẩu</h2>
                        <p>Xin chào,</p>
                        <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản liên kết với email này tại <strong>StudyDocs</strong>.</p>
                        <div style="background-color: #f1f5f9; padding: 15px; border-radius: 6px; text-align: center; margin: 20px 0;">
                            <span style="font-size: 14px; color: #64748b;">Mã xác thực OTP của bạn là:</span>
                            <div style="font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #1e293b; margin-top: 5px;">%s</div>
                        </div>
                        <p style="color: #dc2626; font-size: 13px;">* Mã này sẽ hết hạn sau <strong>%d phút</strong>.</p>
                        <p>Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc đổi mật khẩu để đảm bảo an toàn.</p>
                        <hr style="border: none; border-top: 1px solid #e2e8f0; margin: 20px 0;">
                        <p style="font-size: 12px; color: #94a3b8; text-align: center;">Đây là email tự động từ StudyDocs. Vui lòng không trả lời thư này.</p>
                    </div>
                    """.formatted(otp, expiryMinutes), true);

            mailSender.send(message);
            log.info("[Async Mail] Successfully sent OTP email to {} in {} ms", toEmail, (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            log.error("[Async Mail] Failed to send OTP email to {} after {} ms. Reason: {}",
                    toEmail, (System.currentTimeMillis() - startTime), ex.getMessage(), ex);
        }
    }

    /**
     * Gửi email thông báo đổi mật khẩu thành công bất đồng bộ.
     */
    @Async("mailTaskExecutor")
    public void sendPasswordChangedEmailAsync(String toEmail) {
        long startTime = System.currentTimeMillis();
        log.info("[Async Mail] Sending password-changed notification to {}", toEmail);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            if (fromEmail != null && !fromEmail.isBlank()) {
                helper.setFrom(fromEmail, fromName);
            }
            helper.setTo(toEmail);
            helper.setSubject("Mật khẩu tài khoản StudyDocs của bạn đã được thay đổi");
            helper.setText("""
                    Xin chào,

                    Mật khẩu tài khoản của bạn tại StudyDocs đã được cập nhật thành công.

                    Nếu bạn không thực hiện thay đổi này, hãy liên hệ ngay với quản trị viên hoặc đặt lại mật khẩu ngay lập tức.

                    Trân trọng,
                    Đội ngũ hỗ trợ StudyDocs
                    """);

            mailSender.send(message);
            log.info("[Async Mail] Password changed notification sent to {} in {} ms", toEmail, (System.currentTimeMillis() - startTime));
        } catch (Exception ex) {
            log.error("[Async Mail] Failed to send password changed notification to {}. Reason: {}", toEmail, ex.getMessage(), ex);
        }
    }
}