package thang.bida.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.PasswordResetToken;
import thang.bida.model.User;
import thang.bida.repository.PasswordResetTokenRepository;
import thang.bida.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiry-hours:24}")
    private int tokenExpiryHours;

    @Value("${app.password-reset.otp-expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.email.from:no-reply@bida.com}")
    private String fromEmail;

    /**
     * Gửi email quên mật khẩu với OTP
     */
    @Transactional
    public boolean sendPasswordResetEmail(String email) {
        try {
            System.out.println("DEBUG: Sending password reset email to: " + email);

            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                System.out.println("DEBUG: User not found for email: " + email);
                return false;
            }

            User user = userOptional.get();
            System.out.println("DEBUG: User found - ID: " + user.getId() + ", Name: " + user.getFullName());

            // Xóa token cũ nếu có
            try {
                List<PasswordResetToken> existingTokens = tokenRepository.findAll().stream()
                        .filter(t -> t.getUser().getId().equals(user.getId()))
                        .collect(Collectors.toList());

                if (!existingTokens.isEmpty()) {
                    System.out.println("DEBUG: Found " + existingTokens.size() + " existing tokens to delete");
                    tokenRepository.deleteAll(existingTokens);
                    tokenRepository.flush();
                }
            } catch (Exception e) {
                System.out.println("DEBUG: Error deleting old tokens (might not exist): " + e.getMessage());
            }

            // Tạo OTP 6 số
            String otp = generateOtp();
            System.out.println("DEBUG: Generated OTP: " + otp);

            // Tạo token
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(generateToken());
            resetToken.setUser(user);
            resetToken.setExpiryDate(LocalDateTime.now().plusHours(tokenExpiryHours));
            resetToken.setOtp(otp);
            resetToken.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            resetToken.setUsed(false);

            System.out.println("DEBUG: Saving new token...");
            tokenRepository.save(resetToken);
            tokenRepository.flush(); // Đảm bảo lưu vào DB

            System.out.println("DEBUG: Token saved successfully - ID: " + resetToken.getId());
            System.out.println("DEBUG: Token: " + resetToken.getToken());
            System.out.println("DEBUG: Expiry date: " + resetToken.getExpiryDate());
            System.out.println("DEBUG: OTP expiry date: " + resetToken.getOtpExpiryDate());

            // Gửi email
            System.out.println("DEBUG: Sending email to: " + user.getEmail());
            sendOtpEmail(user.getEmail(), user.getFullName(), otp);
            System.out.println("DEBUG: Email sent successfully");

            return true;
        } catch (Exception e) {
            System.err.println("ERROR in sendPasswordResetEmail: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Xác thực OTP
     */
    @Transactional
    public Optional<String> verifyOtp(String email, String otp) {
        try {
            System.out.println("DEBUG: Verifying OTP - Email: " + email + ", OTP: " + otp);

            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                System.out.println("DEBUG: User not found for email: " + email);
                return Optional.empty();
            }

            User user = userOptional.get();
            System.out.println("DEBUG: User found - ID: " + user.getId());

            // Tìm token theo user và OTP
            List<PasswordResetToken> allTokens = tokenRepository.findAll();

            Optional<PasswordResetToken> tokenOptional = allTokens.stream()
                    .filter(t -> t.getOtp().equals(otp) &&
                            t.getUser().getId().equals(user.getId()) &&
                            !t.isUsed() &&
                            !t.isOtpExpired())
                    .findFirst();

            if (tokenOptional.isEmpty()) {
                System.out.println("DEBUG: No valid token found for OTP: " + otp + " and user ID: " + user.getId());
                return Optional.empty();
            }

            PasswordResetToken token = tokenOptional.get();

            // Kiểm tra OTP hết hạn
            if (token.isOtpExpired()) {
                System.out.println("DEBUG: OTP expired. OTP expiry date: " + token.getOtpExpiryDate());
                return Optional.empty();
            }

            // KHÔNG đánh dấu used ở đây
            System.out.println("DEBUG: OTP verified successfully. Token: " + token.getToken());

            return Optional.of(token.getToken());
        } catch (Exception e) {
            System.err.println("ERROR in verifyOtp: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Đặt lại mật khẩu
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        try {
            System.out.println("=== RESET PASSWORD START ===");
            System.out.println("Token: " + token);

            String cleanToken = token.trim();
            Optional<PasswordResetToken> tokenOptional = tokenRepository.findByToken(cleanToken);

            System.out.println("Token found in DB: " + tokenOptional.isPresent());

            if (tokenOptional.isEmpty()) {
                System.out.println("❌ Token not found in database");
                return false;
            }

            PasswordResetToken resetToken = tokenOptional.get();
            System.out.println("Token details:");
            System.out.println("  ID: " + resetToken.getId());
            System.out.println("  User ID: " + resetToken.getUser().getId());
            System.out.println("  Used: " + resetToken.isUsed());
            System.out.println("  Expiry Date: " + resetToken.getExpiryDate());
            System.out.println("  Is Expired: " + resetToken.isExpired());

            // Kiểm tra token hết hạn
            if (resetToken.isExpired()) {
                System.out.println("❌ Token expired. Expiry date: " + resetToken.getExpiryDate());
                return false;
            }

            // Kiểm tra token đã sử dụng (CHỈ KIỂM TRA NẾU KHÔNG CHO PHÉP REUSE)
            if (resetToken.isUsed()) {
                System.out.println("❌ Token already used");
                return false;
            }

            // Cập nhật mật khẩu
            User user = resetToken.getUser();
            System.out.println("User to update - ID: " + user.getId() + ", Email: " + user.getEmail());

            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            System.out.println("✅ Password updated for user ID: " + user.getId());

            // Đánh dấu token đã sử dụng SAU KHI RESET THÀNH CÔNG
            resetToken.setUsed(true);
            tokenRepository.save(resetToken);
            System.out.println("✅ Token marked as used");

            System.out.println("=== RESET PASSWORD SUCCESS ===");
            return true;

        } catch (Exception e) {
            System.err.println("❌ RESET PASSWORD ERROR: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gửi lại OTP
     */
    @Transactional
    public boolean resendOtp(String email) {
        try {
            Optional<User> userOptional = userRepository.findByEmail(email);
            if (userOptional.isEmpty()) {
                return false;
            }

            User user = userOptional.get();
            Optional<PasswordResetToken> tokenOptional = tokenRepository.findByUserAndUsedFalse(user);

            if (tokenOptional.isEmpty()) {
                return false;
            }

            PasswordResetToken token = tokenOptional.get();

            // Tạo OTP mới
            String newOtp = generateOtp();
            token.setOtp(newOtp);
            token.setOtpExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            tokenRepository.save(token);

            // Gửi email với OTP mới
            sendOtpEmail(user.getEmail(), user.getFullName(), newOtp);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Tạo OTP 6 số
     */
    private String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    /**
     * Tạo token
     */
    private String generateToken() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Gửi email OTP
     */
    private void sendOtpEmail(String toEmail, String userName, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Mã OTP đặt lại mật khẩu - Hệ thống Quản lý Bida");
        message.setText(String.format(
                "Xin chào %s,\n\n" +
                        "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản của mình.\n" +
                        "Mã OTP của bạn là: %s\n" +
                        "Mã OTP có hiệu lực trong %d phút.\n\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\n" +
                        "Đội ngũ Hệ thống Quản lý Bida",
                userName, otp, otpExpiryMinutes));

        mailSender.send(message);
    }

    /**
     * Dọn dẹp token hết hạn (chạy mỗi ngày lúc 2:00 AM)
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteByExpiryDateBefore(now);
    }
}