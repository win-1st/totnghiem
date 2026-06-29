package thang.bida.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import thang.bida.dto.ApiResponse;
import thang.bida.dto.ChangePasswordRequest;
import thang.bida.dto.ForgotPasswordRequest;
import thang.bida.dto.JwtResponse;
import thang.bida.dto.ResetPasswordRequest;
import thang.bida.dto.VerifyOtpRequest;
import thang.bida.model.User;
import thang.bida.dto.LoginRequest;
import thang.bida.dto.RegisterRequest;
import thang.bida.repository.UserRepository;
import thang.bida.security.jwt.JwtUtils;
import thang.bida.services.PasswordResetService;
import thang.bida.services.UserDetailsImpl;
import thang.bida.services.UserService;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final UserDetailsService userDetailsService;

    public AuthController(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            AuthenticationManager authenticationManager,
            PasswordResetService passwordResetService,
            UserService userService,
            UserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.userService = userService;
        this.userDetailsService = userDetailsService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Server is running! Auth controller is healthy.");
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        if (userService.existsByPhone(signUpRequest.getPhone())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Số điện thoại đã được đăng ký!"));
        }

        // Chỉ kiểm tra email nếu email được cung cấp
        if (signUpRequest.getEmail() != null && !signUpRequest.getEmail().isEmpty()
                && userService.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Email đã được sử dụng!"));
        }

        userService.registerUser(signUpRequest);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        System.out.println("=== LOGIN ATTEMPT ===");
        System.out.println("Phone: " + loginRequest.getPhone());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhone(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);
        System.out.println("JWT generated: " + (jwt != null ? "Yes" : "No"));

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());

        System.out.println("User ID: " + userDetails.getId());
        System.out.println("User Phone: " + userDetails.getUsername());
        System.out.println("User FullName: " + userDetails.getFullName());
        System.out.println("Roles: " + roles);

        JwtResponse response = new JwtResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(), // Đây là phone
                userDetails.getFullName(),
                userDetails.getEmail(),
                roles);

        System.out.println("Response created successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("phone", user.getPhone());
        response.put("email", user.getEmail());
        response.put("fullName", user.getFullName());
        response.put("imageUrl", user.getImageUrl());
        response.put("address", user.getAddress());
        response.put("role", user.getRole() != null ? user.getRole().name() : "CUSTOMER");

        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Mật khẩu hiện tại không đúng"));
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken() {
        return ResponseEntity.ok(Map.of("message", "Token is valid"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        return ResponseEntity.ok(Map.of("valid", true, "message", "Token is valid"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.sendPasswordResetEmail(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không tìm thấy email hoặc có lỗi xảy ra. Vui lòng thử lại."));
        }
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request,
            BindingResult bindingResult) {

        System.out.println("=== VERIFY OTP CONTROLLER ===");
        System.out.println("Email: " + request.getEmail());
        System.out.println("OTP: " + request.getOtp());

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage())
                    .collect(Collectors.joining(", "));
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Validation error: " + errorMessage));
        }

        try {
            var tokenOptional = passwordResetService.verifyOtp(request.getEmail(), request.getOtp());

            if (tokenOptional.isPresent()) {
                System.out.println("OTP verification successful. Token: " + tokenOptional.get());
                return ResponseEntity.ok(new ApiResponse(true,
                        "Xác thực OTP thành công",
                        Map.of("token", tokenOptional.get())));
            } else {
                System.out.println("OTP verification failed");
                return ResponseEntity.badRequest().body(new ApiResponse(false,
                        "Mã OTP không hợp lệ hoặc đã hết hạn"));
            }
        } catch (Exception e) {
            System.err.println("Error in verifyOtp controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse(false, "Lỗi server: " + e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        boolean success = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Token không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."));
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse> resendOtp(@Valid @RequestBody ForgotPasswordRequest request) {
        boolean success = passwordResetService.resendOtp(request.getEmail());

        if (success) {
            return ResponseEntity.ok(new ApiResponse(true,
                    "Đã gửi lại mã OTP. Vui lòng kiểm tra email."));
        } else {
            return ResponseEntity.badRequest().body(new ApiResponse(false,
                    "Không thể gửi lại mã OTP. Vui lòng thử lại."));
        }
    }

    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody Map<String, String> profileRequest) {

        try {
            User user = userRepository.findById(userDetails.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (profileRequest.containsKey("fullName")) {
                user.setFullName(profileRequest.get("fullName"));
            }
            if (profileRequest.containsKey("email")) {
                String newEmail = profileRequest.get("email");
                if (!newEmail.equals(user.getEmail()) && userService.existsByEmail(newEmail)) {
                    return ResponseEntity
                            .badRequest()
                            .body(Map.of("message", "Email đã được sử dụng bởi tài khoản khác"));
                }
                user.setEmail(newEmail);
            }

            if (profileRequest.containsKey("address")) {
                user.setAddress(profileRequest.get("address"));
            }

            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật thông tin thành công");
            response.put("data", Map.of(
                    "id", user.getId(),
                    "phone", user.getPhone(), // Vẫn trả về phone nhưng không cho sửa
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "address", user.getAddress(),
                    "role", user.getRole() != null ? user.getRole().name() : "CUSTOMER"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "Cập nhật thất bại: " + e.getMessage()));
        }
    }
}